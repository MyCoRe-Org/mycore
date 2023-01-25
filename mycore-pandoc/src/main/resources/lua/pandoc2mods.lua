require "general"
require "pubtypes"
require "mods"

--[[ Uncomment for debugging
local transform_orig = transform

function transform(o)
    print(dump(o))
    return transform_orig(o)
end

-- The following code will produce runtime warnings when you haven't defined
-- all of the functions you need for the custom writer, so it's useful
-- to include when you're working on a writer.
meta.__index =
  function(_, key)
    io.stderr:write(string.format("WARNING: Undefined function '%s'\n",key))
    return function() return "" end
  end
--]]

defaultGenre = "misc"

function article(o)
    local mods = modsGenre("contribution")
    mods = mods .. modsTitleInfo(o, "title")
    mods = mods .. "<mods:relatedItem type=\"host\">\n"
    mods = mods .. modsGenre("periodical")
    mods = mods .. editors(o)
    mods = mods .. "<mods:relatedItem type=\"host\">\n"
    mods = mods .. modsGenre("series")
    if o['container-title'] ~= nil then
        mods = mods .. modsTitleInfo(o, "container-title");
    elseif o['collection-title'] ~= nil then
        mods = mods .. modsTitleInfo(o, "collection-title");
    end
    mods = mods .. modsPart(o, {"volume", "issue"})
    mods = mods .. modsIdentifier(o,"issn")
    mods = mods .. "</mods:relatedItem>\n"
    mods = mods .. modsPart(o, "page")
    mods = mods .. modsOriginInfo(o)
    mods = mods .. "</mods:relatedItem>\n"
    mods = mods .. modsCommon(o)
    return mods
end

function article_journal(o)
    return article(o)
end

function article_newspaper(o)
    return article(o)
end

function article_magazine(o)
    return article(o)
end

function bookCommon(o)
    local mods = ""
    if o['event'] ~= nil or o['event-place'] ~= nil then
        mods = mods .. modsGenre("proceedings")
        mods = mods .. event(o)
    elseif o['type'] == "entry-encyclopedia" then
        mods = mods .. modsGenre("reference")
    elseif o['editor'] ~= nil then
        mods = mods .. modsGenre("collection")
    else
        mods = mods .. modsGenre("book")
    end
    mods = mods .. container_authors(o)
    mods = mods .. editors(o)
    mods = mods .. translators(o)
    if o['volume-title'] ~= nil then
        mods = mods .. modsTitleInfo(o, "volume-title")
    elseif o['container-title'] ~= nil then
        mods = mods .. modsTitleInfo(o, "container-title")
    elseif o['type'] ~= "chapter" then
        mods = mods .. modsTitleInfo(o, "title")
    end
    if o['collection-title'] ~= nil or o['volume-title'] ~= nil or o['volume'] ~= nil or o['issue'] ~= nil or o['collection-number'] ~= nil then
        mods = mods .. "<mods:relatedItem type=\"host\">\n"
        mods = mods .. modsGenre("series")
        if o['volume-title'] ~= nil then
            if o['type'] == "book" then
                mods = mods .. modsTitleInfo(o, "title")
            end
            mods = mods .. modsTitleInfo(o, "container-title")
        end
        mods = mods .. modsTitleInfo(o, "collection-title")
        mods = mods .. modsPart(o, {"volume", "issue"})
        mods = mods .. "</mods:relatedItem>\n"
    end
    mods = mods .. modsOriginInfo(o)
    mods = mods .. modsIdentifier(o,"isbn")
    return mods
end

function book(o)
    local mods = ""
    if o['page'] ~= nil or o['pages'] ~= nil then
        mods = mods .. chapter(o)
    else
        mods = mods .. bookCommon(o)
        mods = mods .. modsCommon(o)
    end
    return mods
end

function classic(o)
    return book(o)
end

function chapter(o)
    local mods = modsGenre("contribution")
    if o['type'] == "chapter" then
        mods = mods .. modsTitleInfo(o, "title")
    end
    mods = mods .. "<mods:relatedItem type=\"host\">\n"
    mods = mods .. bookCommon(o)
    mods = mods .. modsPart(o, "page")
    mods = mods .. "</mods:relatedItem>\n"
    mods = mods .. modsCommon(o)
    return mods
end

function paper_conference(o)
    return chapter(o)
end

function entry_encyclopedia(o)
    return chapter(o)
end

function periodical(o)
    return chapter(o)
end

function legislation(o)
    local mods = modsGenre("standard")
    mods = mods .. modsTitleInfo(o, "title")
    mods = mods .. modsOriginInfo(o)
    mods = mods .. modsCommon(o)
    return mods
end

function standard(o)
    return legislation(o)
end

