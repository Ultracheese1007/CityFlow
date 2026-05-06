#!/usr/bin/env python3
"""
TSV 清洗脚本：解决"字段内含真换行 / 回车导致行错位"的问题。

策略：
  1. 第一行（header）的 tab 数确定每条记录的"标准列数" N。
  2. 从第二行起逐行扫描：
     - 若当前缓冲为空且本行恰好 N 列 → 视为完整记录，原样输出。
     - 若列数 < N → 视为某条记录被换行截断的开头，进入缓冲。
     - 持续把后续行用空格拼接到缓冲里，直到拼接结果列数 == N，输出后清空缓冲。
  3. 字段内的 \r 一律删除；不动 \t（已经是分隔符语义）。

幂等：清洗已经干净的文件不会改变它的内容（除了去 \r）。
默认对 ./ops/flyway/data/*.tsv 原地清洗，原文件备份为 *.tsv.bak。
"""

import sys
from pathlib import Path


def clean_tsv_text(text: str) -> tuple[str, dict]:
    """返回 (清洗后的文本, 统计信息)"""
    # 先把所有 \r 干掉，统一行尾
    text = text.replace("\r", "")

    raw_lines = text.split("\n")
    # 去掉文末空行
    while raw_lines and raw_lines[-1] == "":
        raw_lines.pop()

    if not raw_lines:
        return "", {"records": 0, "stitched": 0}

    header = raw_lines[0]
    expected = header.count("\t") + 1

    out = [header]
    buf = ""
    stitched = 0

    for line in raw_lines[1:]:
        candidate = line if not buf else (buf + " " + line)
        cols = candidate.count("\t") + 1

        if cols == expected:
            out.append(candidate)
            if buf:
                stitched += 1
            buf = ""
        elif cols < expected:
            buf = candidate
        else:
            # 多于预期列数 —— 数据本身有问题，直接吐出来不让循环卡死
            out.append(candidate)
            buf = ""

    if buf:
        # 文件末尾还有未拼完的残片 —— 也一并输出，方便人工排查
        out.append(buf)

    return "\n".join(out) + "\n", {
        "records": len(out) - 1,
        "stitched": stitched,
        "expected_cols": expected,
    }


def clean_file(p: Path) -> None:
    original = p.read_text(encoding="utf-8")
    cleaned, stats = clean_tsv_text(original)

    if cleaned == original:
        print(f"  ✓ {p.name}: already clean ({stats['records']} records, "
              f"{stats['expected_cols']} cols)")
        return

    backup = p.with_suffix(p.suffix + ".bak")
    if not backup.exists():
        backup.write_text(original, encoding="utf-8")
        print(f"  · {p.name}: backed up to {backup.name}")

    p.write_text(cleaned, encoding="utf-8")
    print(f"  ✓ {p.name}: cleaned ({stats['records']} records, "
          f"{stats['expected_cols']} cols, stitched {stats['stitched']} broken record(s))")


def main():
    # 默认目录：相对仓库根目录的 ops/flyway/data
    data_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("ops/flyway/data")
    if not data_dir.is_dir():
        print(f"ERROR: directory not found: {data_dir}", file=sys.stderr)
        sys.exit(1)

    tsv_files = sorted(data_dir.glob("*.tsv"))
    if not tsv_files:
        print(f"No .tsv files in {data_dir}", file=sys.stderr)
        sys.exit(1)

    print(f"Cleaning TSV files in {data_dir}:")
    for p in tsv_files:
        clean_file(p)
    print("Done.")


if __name__ == "__main__":
    main()
