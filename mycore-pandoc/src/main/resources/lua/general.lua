-- This function is called once for the whole document. Parameters:
-- body is a string, metadata is a table, variables is a table.
function Doc(body, metadata, variables)
    print(transform(metadata))
    return body
end

function pubtype(o)
    local func = o['type']:gsub("-", "_")
    if func == "" then
        func = "default"
    end
    return _G[func](o)
end

function Str(s)
  return escape(s)
end

function Space()
  return " "
end

function Span(s, attr)
  return s
end

function Blocksep()
  return ""
end

function Cite(s, cs)
  return s
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
      return tostring(o)
   end
end

meta = {}
meta.__index =
  function(_, key)
    return function() return "" end
  end
setmetatable(_G, meta)
