stringify = pandoc.utils.stringify

-- This function is called once for the whole document. Parameters:
-- doc is the pandoc AST, doc.meta is a table of metadata, opts contains writer options
-- cf. https://pandoc.org/custom-writers.html
function Writer (doc, opts)
    return transform(doc.meta)
end

function pubtype(o)
    local func = o['type']:gsub("-", "_")
    if func == "" then
        func = "default"
    end
    return _G[func](o)
end

function dumpPandocAst(o)
    if type(o) == 'table' then
        local s = '{ \n'
        for k,v in pairs(o) do
            if type(k) ~= 'number' then k = '"'..k..'"' end
            s = s .. '['..k..'] = ' .. dumpPandocAst(v) .. ',\n'
        end
        return s .. '} '
    else
        return stringify(o)
    end
end

meta = {}
meta.__index =
function(_, key)
    return function() return "" end
end
setmetatable(_G, meta)
