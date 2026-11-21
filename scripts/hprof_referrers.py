#!/usr/bin/env python3
"""Follows references backwards from the big byte arrays of a heap dump to show what holds them.

Usage: hprof_referrers.py <file.hprof> [array_length] [levels]
Each level lists the classes whose instances (or object arrays) point at the level before it. Static fields and
GC roots met on the way are listed too. Plain HPROF as written by ART.
"""
import collections
import mmap
import struct
import sys

PRIM_SIZE = {4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 8}


class Dump:
    def __init__(self, path):
        with open(path, "rb") as handle:
            self.data = mmap.mmap(handle.fileno(), 0, access=mmap.ACCESS_READ)
        d = self.data
        nul = d.find(b"\0")
        self.idsize = struct.unpack_from(">I", d, nul + 1)[0]
        self.start = nul + 1 + 4 + 8
        self.fmt = ">I" if self.idsize == 4 else ">Q"
        self.strings = {}
        self.class_name = {}
        self.classes = {}  # id -> (super, [(name_id, type)], statics [(name_id, value_id)])
        self.objects = []  # (obj id, class id, offset, nbytes)
        self.object_arrays = []  # (obj id, offset, count)
        self.targets = {}
        self.roots = set()

    def rid(self, p):
        return struct.unpack_from(self.fmt, self.data, p)[0]

    def scan(self, array_length):
        d, ids = self.data, self.idsize
        pos = self.start
        load = {}
        while pos < len(d):
            tag = d[pos]
            length = struct.unpack_from(">I", d, pos + 5)[0]
            body = pos + 9
            if tag == 0x01:
                self.strings[self.rid(body)] = bytes(d[body + ids : body + length]).decode("utf-8", "replace")
            elif tag == 0x02:
                load[self.rid(body + 4)] = self.rid(body + 4 + ids + 4)
            elif tag in (0x0C, 0x1C):
                self._segment(body, body + length, array_length)
            pos = body + length
        for cls, name_id in load.items():
            self.class_name[cls] = self.strings.get(name_id, f"class@{cls}")

    def _segment(self, p, stop, array_length):
        d, ids = self.data, self.idsize
        while p < stop:
            sub = d[p]
            p += 1
            if sub in (0xFF, 0x05, 0x07, 0x89, 0x8A, 0x8B, 0x8D, 0x90):
                self.roots.add(self.rid(p))
                p += ids
            elif sub == 0x01:
                self.roots.add(self.rid(p))
                p += 2 * ids
            elif sub in (0x02, 0x03, 0x08):
                self.roots.add(self.rid(p))
                p += ids + 8
            elif sub in (0x04, 0x06):
                self.roots.add(self.rid(p))
                p += ids + 4
            elif sub == 0x8E:
                p += ids + 8
            elif sub == 0xFE:
                p += 4 + ids
            elif sub == 0xC3:
                p += ids + 4 + 4 + 1
            elif sub == 0x20:
                cls = self.rid(p)
                sup = self.rid(p + ids + 4)
                p += ids + 4 + 6 * ids + 4
                pool = struct.unpack_from(">H", d, p)[0]
                p += 2
                for _ in range(pool):
                    kind = d[p + 2]
                    p += 3 + (ids if kind == 2 else PRIM_SIZE[kind])
                statics = []
                count = struct.unpack_from(">H", d, p)[0]
                p += 2
                for _ in range(count):
                    name = self.rid(p)
                    kind = d[p + ids]
                    p += ids + 1
                    if kind == 2:
                        statics.append((name, self.rid(p)))
                        p += ids
                    else:
                        p += PRIM_SIZE[kind]
                fields = []
                count = struct.unpack_from(">H", d, p)[0]
                p += 2
                for _ in range(count):
                    fields.append((self.rid(p), d[p + ids]))
                    p += ids + 1
                self.classes[cls] = (sup, fields, statics)
            elif sub == 0x21:
                obj = self.rid(p)
                cls = self.rid(p + ids + 4)
                nbytes = struct.unpack_from(">I", d, p + 2 * ids + 4)[0]
                self.objects.append((obj, cls, p + 2 * ids + 8, nbytes))
                p += 2 * ids + 8 + nbytes
            elif sub == 0x22:
                obj = self.rid(p)
                count = struct.unpack_from(">I", d, p + ids + 4)[0]
                self.object_arrays.append((obj, p + 2 * ids + 8, count))
                p += 2 * ids + 8 + count * ids
            elif sub == 0x23:
                obj = self.rid(p)
                count, kind = struct.unpack_from(">IB", d, p + ids + 4)
                if kind == 8 and count == array_length:
                    self.targets[obj] = "byte[%d]" % count
                p += ids + 4 + 4 + 1 + count * PRIM_SIZE[kind]
            else:
                raise SystemExit(f"unknown sub-record 0x{sub:02x}")

    def refs_of(self, cls, offset, nbytes):
        """The object ids an instance points at, with the field name."""
        d, ids = self.data, self.idsize
        p, end = offset, offset + nbytes
        c = cls
        while c and c in self.classes and p < end:
            sup, fields, _ = self.classes[c]
            for name_id, kind in fields:
                if kind == 2:
                    yield self.rid(p), self.strings.get(name_id, "?")
                    p += ids
                else:
                    p += PRIM_SIZE[kind]
            c = sup


def main(path, array_length, levels):
    dump = Dump(path)
    dump.scan(array_length)
    print(f"{len(dump.targets)} byte[{array_length}] arrays, {len(dump.objects)} instances, {len(dump.roots)} roots")
    current = set(dump.targets)
    seen = set(current)
    name = dump.class_name
    for level in range(1, levels + 1):
        holders = collections.Counter()
        field_names = collections.Counter()
        found = set()
        for obj, cls, offset, nbytes in dump.objects:
            if obj in seen:
                continue
            for ref, field in dump.refs_of(cls, offset, nbytes):
                if ref in current:
                    holders[name.get(cls, "?")] += 1
                    field_names[(name.get(cls, "?"), field)] += 1
                    found.add(obj)
                    break
        for obj, offset, count in dump.object_arrays:
            if obj in seen:
                continue
            for i in range(count):
                if dump.rid(offset + i * dump.idsize) in current:
                    holders["object[] array"] += 1
                    found.add(obj)
                    break
        statics = [
            (name.get(cls, "?"), dump.strings.get(n, "?"))
            for cls, (_, _, sts) in dump.classes.items()
            for n, value in sts
            if value in current
        ]
        print(f"level {level}: {len(found)} holders")
        for cls, count in holders.most_common(8):
            print(f"   {cls} x{count}")
        for (cls, field), count in field_names.most_common(4):
            print(f"   via field {cls}.{field} x{count}")
        for owner, field in statics[:5]:
            print(f"   STATIC {owner}.{field}")
        rooted = len(found & dump.roots)
        print(f"   of which GC roots: {rooted}")
        if not found:
            break
        seen |= found
        current = found


if __name__ == "__main__":
    main(sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 2592000, int(sys.argv[3]) if len(sys.argv) > 3 else 7)
