-- EndnoteXML: Conference Paper, Electronic Article
-- RIS:        ABST, EJOUR, SER
function article(o)
    return default(o)
end

-- Bib(La)TeX: article, periodical, suppperiodical
-- EndnoteXML: Journal Article
-- RIS:        INPR, JFULL, JOUR
function article_journal(o)
    return default(o)
end

-- Bib(La)TeX: article, periodical, suppperiodical (with entrysubtype=magazine)
-- EndnoteXML: Magazine Article
-- RIS:        MGZN
function article_magazine(o)
    return default(o)
end

-- Bib(La)TeX: article, periodical, suppperiodical (with entrysubtype=newspaper), newsarticle
-- EndnoteXML: Newspaper Article
-- RIS:        NEWS
function article_newspaper(o)
    return default(o)
end

-- RIS:        BILL, UNBILL
function bill(o)
    return default(o)
end

-- Bib(La)TeX: book, collection, manual, mvbook, mvcollection, mvproceedings, mvreference, proceedings, reference,
--             software, commentary
-- EndnoteXML: Book, Dictionary, Edited Book, Electronic Book, Encyclopedia
-- RIS:        ANCIENT, BOOK, CLSWK, DICT, EBOOK, EDBOOK
function book(o)
    return default(o)
end

-- RIS:        CTLG
function catalog(o)
    return default(o)
end

-- Bib(La)TeX: bookinbook, inbook, incollection, suppbook, suppcollection
-- EndnoteXML: Book Section, Electronic Book Section
-- RIS:        CHAP, ECHAP
function chapter(o)
    return default(o)
end

-- EndnoteXML: Ancient Text, Classical Work
function classic(o)
    return default(o)
end

-- Bib(La)TeX: dataset, data
-- EndnoteXML: Aggregated Database, Dataset, Online Database
-- RIS:        AGGR, DATA, DBASE
function dataset(o)
    return default(o)
end

-- EndnoteXML: Catalog, Equation, Government Document, Grant, Podcast
function document(o)
    return default(o)
end

-- RIS:        GEN
function entry(o)
    return default(o)
end

-- Bib(La)TeX: inreference
-- RIS:        ENCYC
function entry_encyclopedia(o)
    return default(o)
end

-- RIS:        EQUA, FIGURE
function figure(o)
    return default(o)
end

-- Bib(La)TeX: artwork, image
-- EndnoteXML: Artwork, Audiovisual Material, Chart or Table, Figure
-- RIS:        ART, CHART, SLIDE, VIDEO
function graphic(o)
    return default(o)
end

-- EndnoteXML: Hearing
function hearing(o)
    return default(o)
end

-- EndnoteXML: Interview
function interview(o)
    return default(o)
end

-- Bib(La)TeX: jurisdiction
-- EndnoteXML: Case
-- RIS:        CASE, LEGAL
function legal_case(o)
    return default(o)
end

-- Bib(La)TeX: legislation, standard
-- EndnoteXML: Bill, Statute
-- RIS:        STAT
function legislation(o)
    return default(o)
end

-- Bib(La)TeX: unpublished
-- EndnoteXML: Manuscript
-- RIS:        MANSCPT, UNPB
function manuscript(o)
    return default(o)
end

-- RIS:        MAP
-- EndnoteXML: Map
function map(o)
    return default(o)
end

-- Bib(La)TeX: movie, video
-- EndnoteXML: Film or Broadcast
-- RIS:        ADVS, MPCT
function motion_picture(o)
    return default(o)
end

-- RIS:        MUSIC, SOUND
-- EndnoteXML: Music
function musical_score(o)
    return default(o)
end

-- Bib(La)TeX: booklet
-- EndnoteXML: Pamphlet
-- RIS:        PAMP
function pamphlet(o)
    return default(o)
end

-- Bib(La)TeX: inproceedings
-- RIS:        CONF, CPAPER
function paper_conference(o)
    return default(o)
end

-- Bib(La)TeX: patent
-- EndnoteXML: Patent
-- RIS:        PAT
function patent(o)
    return default(o)
end

-- EndnoteXML: Conference Proceedings, Serial
function periodical(o)
    return default(o)
end

-- Bib(La)TeX: letter, letters
-- EndnoteXML: Personal Communication
-- RIS:        ICOMM, PCOMM
function personal_communication(o)
    return default(o)
end

-- RIS:        BLOG
-- EndnoteXML: Blog
function post_weblog(o)
    return default(o)
end

-- RIS:        COMP
function program(o)
    return default(o)
end

-- EndnoteXML: Legal Rule or Regulation
function regulation(o)
    return default(o)
end

-- Bib(La)TeX: report, techreport
-- EndnoteXML: Press Release, Report
-- RIS:        GOVDOC, GRANT, HEAR, RPRT, STAND
function report(o)
    return default(o)
end

-- Bib(La)TeX: review
function review(o)
    return default(o)
end

-- EndnoteXML: Computer program
function software(o)
    return default(o)
end

-- Bib(La)TeX: audio, music
function song(o)
    return default(o)
end

-- Bib(La)TeX: performance, unpublished (when isEvent=speech)
function speech(o)
    return default(o)
end

-- EndnoteXML: Standard
function standard(o)
    return default(o)
end

-- Bib(La)TeX: mastersthesis, phdthesis, thesis
-- EndnoteXML: Thesis
-- RIS:        THES
function thesis(o)
    return default(o)
end

-- Bib(La)TeX: legal
function treaty(o)
    return default(o)
end

-- EndnoteXML: Unpublished Work
function unpublished(o)
    return default(o)
end

-- Bib(La)TeX: electronic, online, www
-- EndnoteXML: Online Multimedia, Web Page
-- RIS:        WEB, MULTI
function webpage(o)
    return default(o)
end
