# You should install librsvg2-bin.

# To do that, run:
# sudo apt install librsvg2-bin

pandoc --template=template.tex --bibliography=bibfile.bib report-config.md overview.md protocols.md chord.md data-storage.md system-storage.md main.md concurrency.md jsse.md scalability.md fault-tolerance.md bibliography.md -o report.pdf
