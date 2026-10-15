#!/usr/bin/env python
"""查询标签表（验证用）。用法: python tools/query_tags.py <db> [CATEGORY]"""
import sqlite3
import sys

db = sys.argv[1]
cat = sys.argv[2] if len(sys.argv) > 2 else None
con = sqlite3.connect(db)
q = "select tagId, name, sortOrder from tags"
if cat:
    q += " where category='" + cat + "'"
q += " order by category, sortOrder limit 20"
for row in con.execute(q):
    print("   ", row)
