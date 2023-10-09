-- Entry point
function transform(o)
    return modsCollection(o)
end

function dump(o)
    return "<!-- " .. dumpPandocAst(o) .. " -->"
end

function modsCollection(o)
    local mods = "<mods:modsCollection xmlns:mods=\"http://www.loc.gov/mods/v3\">\n"
    for _,v in pairs(o['references']) do
        mods = mods .. modsEntry(v)
    end
    mods = mods .. "</mods:modsCollection>"
    return mods
end

function modsEntry(o)
    local mods = "<mods:mods>\n"
    mods = mods .. pubtype(o)
    mods = mods .. "</mods:mods>\n"
    return mods
end

function modsGenre(o)
    if o ~= nil then
        return "<mods:genre type=\"intern\">" .. o .. "</mods:genre>\n"
    end
end

function modsTitleInfo(o, t)
    local mods = ""
    if o[t] ~= nil then
        mods = mods .. "<mods:titleInfo>\n"
        if o[t .. '-short'] ~= nil and escape(stringify(o[t])):sub(1,string.len(escape(stringify(o[t .. '-short']))))
            == escape(stringify(o[t .. '-short'])) then
            mods = mods .. "<mods:title>" .. escape(stringify(o[t .. '-short'])) .. "</mods:title>\n"
            mods = mods .. "<mods:subTitle>" .. escape(stringify(o[t])):sub(string.len(escape(
            stringify(o[t .. '-short'])))+3,string.len(escape(stringify(o[t]))))
            mods = mods .. "</mods:subTitle>\n"
        else
            mods = mods .. "<mods:title>" .. escape(stringify(o[t])) .. "</mods:title>\n"
        end
        mods = mods .. "</mods:titleInfo>\n"
    end
    if o[t .. '-short'] ~= nil then
        mods = mods .. "<mods:titleInfo type=\"abbreviated\">\n<mods:title>" .. escape(stringify(o[t .. '-short']))
        mods = mods .. "</mods:title>\n</mods:titleInfo>\n"
    end
    return mods
end

function persons(o, r, m)
    local a = ""
    if o[r] ~= nil then
        for _,v in pairs(o[r]) do
            a = a .. modsName(v, "personal", m)
        end
    end
    return a
end

function authors(o)
    return persons(o, 'author', 'aut')
end

function container_authors(o)
    return persons(o, 'container-author', 'aut')
end

function editors(o)
    return persons(o, 'editor', 'edt')
end

function translators(o)
    return persons(o, 'translator', 'trl')
end

function editors(o)
    return persons(o, 'editor', 'edt')
end

function event(o)
    return modsName(o, 'conference')
end

function modsName(o, t, r)
    local mods = "<mods:name type=\"" .. t .. "\">\n"
    if t == "personal" then
        mods = mods .. person(o)
        mods = mods .. modsRole(r)
    elseif t == "conference" then
        mods = mods .. conference(o)
    end
    if mods == "<mods:name type=\"" .. t .. "\">\n" then
        return ""
    end
    mods = mods .. "</mods:name>\n"
    return mods
end

function person(o)
    return modsNamePart(o, 'family') .. modsNamePart(o, 'given')
end

function conference(o)
    local event = ""
    if o['event'] ~= nil then
        event = escape(stringify(o['event']))
        if o['event-place'] ~= nil then
            event = event .. ", " .. escape(stringify(o['event-place']))
        end
        if o['event-date'] ~= nil then
            event = event .. ", " .. escape(stringify(o['event-date']))
        end
        event = modsNamePart(o, event)
    end
    return event
end

function modsNamePart(o, t)
    local mods = ""
    if o[t] ~= nil then
        mods = mods .. "<mods:namePart type=\"" .. t .. "\">" .. escape(o[t]) .. "</mods:namePart>\n"
    else
        mods = mods .. "<mods:namePart>" .. escape(t) .. "</mods:namePart>\n"
    end
    return mods
end

function modsRole(s)
    return "<mods:role>\n<mods:roleTerm type=\"code\" authority=\"marcrelator\">" .. s
    ..  "</mods:roleTerm>\n</mods:role>\n"
end

function modsDateIssued(o)
    local mods = ""
    if o['issued'] ~= nil then
        mods = mods .. "<mods:dateIssued encoding=\"w3cdtf\">" .. escape(o['issued']) .. "</mods:dateIssued>\n"
    end
    return mods
end

function modsPublisher(o)
    local mods = ""
    if o['publisher'] ~= nil then
        mods = mods .. "<mods:publisher>" .. escape(stringify(o['publisher'])) .. "</mods:publisher>\n"
    end
    return mods
end

function modsPlace(o)
    local mods = ""
    if o['publisher-place'] ~= nil then
        mods = mods .. "<mods:place>\n<mods:placeTerm type=\"text\">" .. escape(stringify(o['publisher-place']))
        mods = mods .. "</mods:placeTerm>\n</mods:place>\n"
    end
    return mods
end

function modsEdition(o)
    local mods = ""
    if o['edition'] then
        mods = mods .. "<mods:edition>" .. o['edition'] .. "</mods:edition>\n"
    end
    return mods
end

function modsOriginInfo(o)
    local mods = ""
    if o['issued'] ~= nil or o['publisher'] ~= nil or o['publisher-place'] ~= nil or o['edition'] ~= nil then
        mods = mods .. "<mods:originInfo>\n"
        mods = mods .. modsDateIssued(o)
        mods = mods .. modsPublisher(o)
        mods = mods .. modsPlace(o)
        mods = mods .. modsEdition(o)
        mods = mods .. "</mods:originInfo>\n"
    end
    return mods
end

function modsLocation(o)
    local mods = ""
    if o['url'] ~= nil then
        mods = mods .. "<mods:location>\n<mods:url>" .. escape(o['url']) .. "</mods:url>\n</mods:location>\n"
    end
    return mods
end

function modsIdentifier(o,id)
    local mods = ""
    if o[id] ~= nil then
        mods = mods .. "<mods:identifier type=\"" .. id .. "\">" .. escape(o[id]) .. "</mods:identifier>\n"
    elseif o[id:upper()] ~= nil then
        mods = mods .. "<mods:identifier type=\"" .. id .. "\">" .. escape(o[id:upper()]) .. "</mods:identifier>\n"
    end
    return mods
end

function pages(o)
    local mods = ""
    local p = ""
    if o['page'] ~= nil then
        p = escape(stringify(o['page']))
    elseif o['pages'] ~= nil then
        p = escape(stringify(o['pages']))
    end
    if p ~= "" then
        local s,e = p:match("([^\\-]+)[\\-]*(.*)")
        mods = mods .. "<mods:extent unit=\"pages\">\n"
        mods = mods .. "<mods:start>" .. s .. "</mods:start>\n"
        if e ~= "" then
            mods = mods .. "<mods:end>" .. e .. "</mods:end>\n"
        end
        mods = mods .. "</mods:extent>\n"
    end
    return mods
end

function issue(o)
    local mods = ""
    local i = ""
    if o['issue'] ~= nil then
        i = escape(stringify(o['issue']))
    elseif o['collection-number'] ~= nil then
        i = escape(stringify(o['collection-number']))
    end
    if i ~= "" then
        mods = mods .. "<mods:detail type=\"issue\">\n"
        mods = mods .. "<mods:number>" .. i .. "</mods:number>\n"
        mods = mods .. "</mods:detail>\n"
    end
    return mods
end

function volume(o)
    local mods = ""
    if o['volume'] ~= nil then
        mods = mods .. "<mods:detail type=\"volume\">\n"
        mods = mods .. "<mods:number>" .. escape(stringify(o['volume'])) .. "</mods:number>\n"
        mods = mods .. "</mods:detail>\n"
    end
    return mods
end

function modsPart(o,k)
    local mods = "<mods:part>\n"
    if k[1] == nil then
        k = {k}
    end
    for _,j in ipairs(k) do
        if string.match(j, "page") then
            mods = mods .. pages(o)
        end
        if string.match(j, "issue") then
            mods = mods .. issue(o)
        end
        if string.match(j, "volume") then
            mods = mods .. volume(o)
        end
    end
    if mods == "<mods:part>\n" then
        return ""
    else
        return mods .. "</mods:part>\n"
    end
end

function modsNote(o)
    local mods = ""
    if o['note'] ~= nil then
        mods = mods .. "<mods:note>" .. escape(stringify(o['note'])) .. "</mods:note>\n"
    end
    return mods
end

function modsCommon(o)
    local mods = authors(o)
    mods = mods .. modsLocation(o)
    mods = mods .. modsIdentifier(o,"doi")
    mods = mods .. modsNote(o)
    return mods
end

function default(o)
    local genre = defaultGenre
    if defaultGenre == nil and o['type'] ~= "" then
        genre = o['type']
    end
    local mods = modsGenre(genre)
    mods = mods .. modsTitleInfo(o, "title")
    mods = mods .. modsOriginInfo(o)
    mods = mods .. modsCommon(o)
    return mods
end

-- Required escapes.
-- Ampersand ('&') MUST be the first element added to table `escapes`.
local escapes = {}
escapes["&"] = "&amp;"
escapes["<"] = "&lt;"
escapes[">"] = "&gt;"
function escape(s)
    return s:gsub('[<>&]',
    function(x)
        return escapes[x]
    end)
end
