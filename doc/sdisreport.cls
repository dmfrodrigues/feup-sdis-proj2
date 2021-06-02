\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{sdisreport}
\LoadClass[a4paper,11pt]{report}
% Margins
\RequirePackage[top=20mm,bottom=20mm,left=20mm,right=20mm,headsep=4mm]{geometry}
% % Time and date
\usepackage[yyyymmdd,hhmmss]{datetime}
\renewcommand{\dateseparator}{-}
% Headers and footers
\RequirePackage{fancyhdr}
\pagestyle{fancy}
\fancyhf{}
\renewcommand{\headrulewidth}{0pt}
\renewcommand{\footrulewidth}{0pt}
\fancyhead[LE, LO]{\textit{\leftmark}}
\fancyfoot[RE, RO]{\thepage}
\fancyfoot[LE, LO]{\ifx\shortauthors\@empty\else\shortauthors\fi}
