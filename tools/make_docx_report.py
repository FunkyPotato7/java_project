from __future__ import annotations

from html import escape
from pathlib import Path
from shutil import copyfile
from zipfile import ZIP_DEFLATED, ZipFile
import re
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
SAMPLE = Path("D:/labs/4К/Java_Буньо_Лаб.1.docx")
OUT_PROJECT = ROOT / "Java_Буньо_Проєкт_RenovaCalc.docx"
OUT_COPY = Path("D:/labs/4К/Java_Буньо_Проєкт_RenovaCalc.docx")
REPORT = ROOT / "REPORT.md"

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NS = {"w": W_NS}


def run_props(size: str = "28", bold: bool = False, italic: bool = False) -> str:
    props = [
        '<w:rPr>',
        '<w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/>',
        f'<w:sz w:val="{size}"/><w:szCs w:val="{size}"/>',
    ]
    if bold:
        props.append("<w:b/><w:bCs/>")
    if italic:
        props.append("<w:i/><w:iCs/>")
    props.append("</w:rPr>")
    return "".join(props)


def paragraph(
    text: str = "",
    *,
    align: str | None = None,
    bold: bool = False,
    italic: bool = False,
    size: str = "28",
    spacing_after: str = "120",
    first_line: bool = False,
) -> str:
    ppr = ["<w:pPr>"]
    if align:
        ppr.append(f'<w:jc w:val="{align}"/>')
    if first_line:
        ppr.append('<w:ind w:firstLine="708"/>')
    ppr.append(f'<w:spacing w:after="{spacing_after}" w:line="360" w:lineRule="auto"/>')
    ppr.append("</w:pPr>")
    if not text:
        return "<w:p>" + "".join(ppr) + "</w:p>"
    return (
        "<w:p>"
        + "".join(ppr)
        + "<w:r>"
        + run_props(size=size, bold=bold, italic=italic)
        + f'<w:t xml:space="preserve">{escape(text)}</w:t></w:r></w:p>'
    )


def page_break() -> str:
    return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>'


def placeholder(title: str) -> str:
    return f"""
<w:tbl>
  <w:tblPr>
    <w:tblW w:w="0" w:type="auto"/>
    <w:tblBorders>
      <w:top w:val="single" w:sz="12" w:space="0" w:color="808080"/>
      <w:left w:val="single" w:sz="12" w:space="0" w:color="808080"/>
      <w:bottom w:val="single" w:sz="12" w:space="0" w:color="808080"/>
      <w:right w:val="single" w:sz="12" w:space="0" w:color="808080"/>
    </w:tblBorders>
  </w:tblPr>
  <w:tr>
    <w:trPr><w:trHeight w:val="3600" w:hRule="atLeast"/></w:trPr>
    <w:tc>
      <w:tcPr><w:tcW w:w="9360" w:type="dxa"/><w:vAlign w:val="center"/></w:tcPr>
      {paragraph(title, align="center", bold=True, spacing_after="80")}
      {paragraph("Місце для скриншота", align="center", italic=True, spacing_after="80")}
    </w:tc>
  </w:tr>
</w:tbl>
{paragraph("", spacing_after="160")}
"""


def normalize_inline_markdown(text: str) -> str:
    text = text.replace("**", "")
    text = text.replace("`", "")
    return text


def make_body_from_report() -> list[str]:
    lines = REPORT.read_text(encoding="utf-8").splitlines()
    body: list[str] = [
        paragraph("Звіт про виконання проєкту", align="center", bold=True),
        paragraph("З дисципліни Програмування мовою Java", align="center"),
        paragraph("на тему", align="center"),
        paragraph(
            "«RenovaCalc: калькулятор вартості ремонту з графічним інтерфейсом користувача»",
            align="center",
            bold=True,
        ),
        paragraph("студента групи ІН-2226Б", align="center"),
        paragraph("Буньо Андрія", align="center"),
        paragraph("2026", align="center"),
        page_break(),
    ]

    in_code = False
    figure_index = 1
    skip_next_image = False
    for raw in lines:
        line = raw.strip()
        if line.startswith("# "):
            continue
        if line.startswith("```"):
            in_code = not in_code
            continue
        if not line:
            body.append(paragraph("", spacing_after="60"))
            continue
        if line.startswith("!["):
            continue
        if line in {"Початковий екран:", "Приклад розрахованого кошторису:"}:
            title = f"Рисунок {figure_index} - {line[:-1]}"
            body.append(placeholder(title))
            figure_index += 1
            skip_next_image = True
            continue
        if skip_next_image:
            skip_next_image = False
        if line.startswith("## "):
            body.append(paragraph(line[3:], bold=True, spacing_after="120"))
            continue
        if re.match(r"^\d+\. ", line):
            body.append(paragraph(normalize_inline_markdown(line), spacing_after="80"))
            continue
        if line.startswith("- "):
            body.append(paragraph("- " + normalize_inline_markdown(line[2:]), spacing_after="80"))
            continue
        body.append(
            paragraph(
                normalize_inline_markdown(line),
                align="center" if in_code else None,
                italic=in_code,
                first_line=not in_code,
            )
        )
    return body


def main() -> None:
    with ZipFile(SAMPLE, "r") as zin:
        document = ET.fromstring(zin.read("word/document.xml"))
        body = document.find("w:body", NS)
        sect_pr = body.find("w:sectPr", NS) if body is not None else None
        sect_xml = ET.tostring(sect_pr, encoding="unicode") if sect_pr is not None else "<w:sectPr/>"

        new_document_xml = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="{W_NS}" xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas" xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math" xmlns:v="urn:schemas-microsoft-com:vml" xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:w10="urn:schemas-microsoft-com:office:word" xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml" xmlns:w15="http://schemas.microsoft.com/office/word/2012/wordml" xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup" xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk" xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml" xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape" mc:Ignorable="w14 w15 wp14">
  <w:body>
    {''.join(make_body_from_report())}
    {sect_xml}
  </w:body>
</w:document>"""

        with ZipFile(OUT_PROJECT, "w", ZIP_DEFLATED) as zout:
            for item in zin.infolist():
                data = zin.read(item.filename)
                if item.filename == "word/document.xml":
                    data = new_document_xml.encode("utf-8")
                zout.writestr(item, data)

    copyfile(OUT_PROJECT, OUT_COPY)
    print(OUT_PROJECT)
    print(OUT_COPY)


if __name__ == "__main__":
    main()
