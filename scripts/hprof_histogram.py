#!/usr/bin/env python3
"""Prints what fills a Java heap dump: instances per class and primitive arrays per type and size.

Usage: hprof_histogram.py <file.hprof> [top]. It reads the standard HPROF format that ART writes after
`hprof-conv`-free `am dumpheap`, with Android's extra sub-record tags.
"""
import collections
import mmap
import struct
import sys

PRIM_SIZE = {4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 8}
PRIM_NAME = {4: "boolean", 5: "char", 6: "float", 7: "double", 8: "byte", 9: "short", 10: "int", 11: "long"}


def main(path, top):
    with open(path, "rb") as handle:
        data = mmap.mmap(handle.fileno(), 0, access=mmap.ACCESS_READ)
    end_of_header = data.find(b"\0")
    id_size = struct.unpack_from(">I", data, end_of_header + 1)[0]
    pos = end_of_header + 1 + 4 + 8
    ident = ">I" if id_size == 4 else ">Q"

    def rid(p):
        return struct.unpack_from(ident, data, p)[0]

    strings, class_name_id, class_by_obj = {}, {}, {}
    instances = collections.Counter()
    instance_bytes = collections.Counter()
    arrays = collections.Counter()
    array_bytes = collections.Counter()
    object_arrays = collections.Counter()
    size = len(data)

    while pos < size:
        tag = data[pos]
        length = struct.unpack_from(">I", data, pos + 5)[0]
        body = pos + 9
        if tag == 0x01:
            strings[rid(body)] = bytes(data[body + id_size : body + length]).decode("utf-8", "replace")
        elif tag == 0x02:
            class_by_obj[rid(body + 4)] = rid(body + 4 + id_size + 4)
        elif tag in (0x0C, 0x1C):
            p, stop = body, body + length
            while p < stop:
                sub = data[p]
                p += 1
                if sub in (0xFF, 0x05, 0x07, 0x89, 0x8A, 0x8B, 0x8D, 0x90):
                    p += id_size
                elif sub == 0x01:
                    p += 2 * id_size
                elif sub in (0x02, 0x03, 0x08):
                    p += id_size + 8
                elif sub in (0x04, 0x06):
                    p += id_size + 4
                elif sub == 0x8E:
                    p += id_size + 8
                elif sub == 0xFE:
                    p += 4 + id_size
                elif sub == 0xC3:
                    count, kind = struct.unpack_from(">IB", data, p + id_size + 4)
                    p += id_size + 4 + 4 + 1
                    arrays[(PRIM_NAME.get(kind, "?"), count)] += 1
                elif sub == 0x20:
                    p += id_size + 4 + 6 * id_size + 4
                    pool = struct.unpack_from(">H", data, p)[0]
                    p += 2
                    for _ in range(pool):
                        kind = data[p + 2]
                        p += 3 + (id_size if kind == 2 else PRIM_SIZE[kind])
                    statics = struct.unpack_from(">H", data, p)[0]
                    p += 2
                    for _ in range(statics):
                        kind = data[p + id_size]
                        p += id_size + 1 + (id_size if kind == 2 else PRIM_SIZE[kind])
                    fields = struct.unpack_from(">H", data, p)[0]
                    p += 2 + fields * (id_size + 1)
                elif sub == 0x21:
                    cls = rid(p + id_size + 4)
                    nbytes = struct.unpack_from(">I", data, p + 2 * id_size + 4)[0]
                    instances[cls] += 1
                    instance_bytes[cls] += nbytes
                    p += 2 * id_size + 8 + nbytes
                elif sub == 0x22:
                    count = struct.unpack_from(">I", data, p + id_size + 4)[0]
                    cls = rid(p + id_size + 8)
                    object_arrays[cls] += count
                    p += id_size + 8 + id_size + count * id_size
                elif sub == 0x23:
                    count, kind = struct.unpack_from(">IB", data, p + id_size + 4)
                    nbytes = count * PRIM_SIZE[kind]
                    arrays[(PRIM_NAME[kind], count)] += 1
                    array_bytes[(PRIM_NAME[kind], count)] += nbytes
                    p += id_size + 4 + 4 + 1 + nbytes
                else:
                    print(f"unknown sub-record 0x{sub:02x} at {p}", file=sys.stderr)
                    return
        pos = body + length

    def name(cls_obj):
        return strings.get(class_name_id.get(cls_obj, 0), None) or strings.get(class_by_obj.get(cls_obj, 0), f"class@{cls_obj}")

    print("primitive arrays by total bytes (type, length, how many, MB):")
    for (kind, count), total in array_bytes.most_common(top):
        print(f"  {kind}[{count}] x{arrays[(kind, count)]} = {total / 1048576:.1f} MB")
    print("instances by class (how many, bytes of fields):")
    for cls, total in instance_bytes.most_common(top):
        print(f"  {name(cls)} x{instances[cls]} = {total / 1024:.0f} KB")
    print("object arrays by class (total elements):")
    for cls, count in object_arrays.most_common(5):
        print(f"  {name(cls)} elements {count}")


if __name__ == "__main__":
    main(sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 12)
