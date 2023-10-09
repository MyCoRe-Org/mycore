/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

const baseURL = document.currentScript.getAttribute('baseURL')
const colors = document.currentScript.getAttribute('colors')
const id = document.currentScript.getAttribute('id') ?? null

let network = null

document.addEventListener('DOMContentLoaded', async (event) => {
  const gravitySlider = document.getElementById('gravity-slider')
  const limitSlider = document.getElementById('limit-slider')
  updateSliderValue('gravity', gravitySlider.value)
  updateSliderValue('limit', limitSlider.value)

  if (id) {
    setIsLoading(true)
    const json = await loadData(id)
    setGraph(json)
    setIsLoading(false)
  }
})

const addEdges = (edges, newEdges) => {
  const filteredEdges = []

  newEdges.forEach((newEdge) => {
    const edgeExist = edges.get({ filter: (edge) => edge.from === newEdge.from && edge.to === newEdge.to })?.length

    if (!edgeExist) {
      newEdge.label = newEdge.type
      filteredEdges.push(newEdge)
    }
  })

  edges.add(filteredEdges)
}

const addNodes = (nodes, newNodes, position = undefined) => {
  const filteredNodes = []
  const foundNodes = []

  colors?.split(';')?.forEach((color) => {
    const split = color.split(':')

    if (split.length === 2) {
      foundNodes.push({ label: split[0], color: `#${split[1]}` })
    }
  })

  newNodes.forEach((newNode) => {
    const nodeExist = nodes.get({ filter: (node) => node.id === newNode.id })?.length

    if (!nodeExist) {
      newNode.label = newNode.type

      if (position) {
        newNode.x = position.x
        newNode.y = position.y
      }

      const existingNode = nodes.get({ filter: (node) => node.label === newNode.label, first: true })?.[0]
      const foundNode = foundNodes.find((type) => type.label == newNode.label)

      if (foundNode) {
        newNode.color = foundNode.color
      } else if (existingNode) {
        newNode.color = existingNode.color
      } else {
        const randomColor = generateRandomColor(foundNodes)
        newNode.color = randomColor
        foundNodes.push(newNode)
      }

      if (getBrightness(newNode.color) < 128) {
        newNode.font = {
          color: '#ffffff'
        }
      }

      filteredNodes.push(newNode)
    }
  })

  nodes.add(filteredNodes)
}

const generateRandomColor = (types) => {
  let colorExists = true
  let colorIsDifferent = false
  let randomColor = undefined

  while (colorExists || !colorIsDifferent) {
    randomColor = `#${Math.floor(Math.random() * 16777215)
      .toString(16)
      .padStart(6, '0')}`
    colorExists = types.find((type) => type.color === randomColor) || false

    if (colorExists) {
      continue
    }

    if (!types.length) {
      colorIsDifferent = true
    }

    for (const type of types) {
      colorIsDifferent = isColorDifferent(type.color, randomColor, 128)

      if (!colorIsDifferent) {
        break
      }
    }
  }

  return randomColor
}

const getBrightness = (hexColor) => {
  const red = parseInt(hexColor.substr(1, 2), 16)
  const green = parseInt(hexColor.substr(3, 2), 16)
  const blue = parseInt(hexColor.substr(5, 2), 16)

  return (red + green + blue) / 3
}

const isColorDifferent = (color1, color2, threshold) => {
  const distance = Math.sqrt(
    Math.pow(parseInt(color1.slice(1, 3), 16) - parseInt(color2.slice(1, 3), 16), 2) +
      Math.pow(parseInt(color1.slice(3, 5), 16) - parseInt(color2.slice(3, 5), 16), 2) +
      Math.pow(parseInt(color1.slice(5, 7), 16) - parseInt(color2.slice(5, 7), 16), 2)
  )

  return distance > threshold
}

const loadData = async (id) => {
  const urlPath = `${baseURL}servlets/Neo4JProxyServlet?`
  const limitSlider = document.querySelector('#limit-slider')
  const limit = limitSlider.value

  const urlParams = { id: id }

  if (limit < 1000) {
    urlParams.limit = limit
  }

  try {
    const response = await fetch(`${urlPath}${new URLSearchParams(urlParams)}`)

    if (response.status === 200) {
      return await response.json()
    }
  } catch (error) {
    console.log(error)
  }

  return undefined
}

const resetGraph = async () => {
  if (id) {
    setIsLoading(true)
    const json = await loadData(id)
    setGraph(json)
    setIsLoading(false)
  }
}

const setGraph = (json) => {
  const container = document.getElementById('graph')
  const nodes = new vis.DataSet()
  const edges = new vis.DataSet()

  container.innerHTML = ''

  addNodes(nodes, json.nodes)
  addEdges(edges, json.relations)

  const data = {
    nodes: nodes,
    edges: edges
  }

  const options = {
    edges: {
      arrows: 'to',
      smooth: false
    },
    nodes: {
      font: {
        size: 10
      },
      shape: 'circle',
      widthConstraint: 30
    },
    interaction: {
      hover: true,
      tooltipDelay: 300
    },
    layout: {
      improvedLayout: true
    },
    physics: {
      enabled: true,
      barnesHut: {
        gravitationalConstant: -2000,
        centralGravity: 0.3,
        springLength: 100,
        springConstant: 0.04,
        damping: 0.09,
        avoidOverlap: 0.2
      },
      stabilization: {
        iterations: 1,
        fit: true
      }
    }
  }

  network = new vis.Network(container, data, options)

  network.on('click', async (event) => {
    const nodeId = event.nodes?.[0]

    if (nodeId) {
      const node = nodes.get(nodeId)
      const id = node.mcrid
      setMetaData(node)

      const position = network.canvas.DOMtoCanvas({ x: event.pointer.DOM.x, y: event.pointer.DOM.y })

      setIsLoading(true)
      const json = await loadData(id)

      addNodes(nodes, json.nodes, position)
      addEdges(edges, json.relations)
      setIsLoading(false)
    } else {
      setMetaData(null)
    }
  })

  network.on('hoverNode', (event) => {
    const nodeId = event.node

    if (nodeId) {
      const node = nodes.get(nodeId)
      setMetaData(node)
    }
  })
}

const setGravity = (value) => {
  if (network) {
    network.enableEditMode()
    network.physics.options.barnesHut.gravitationalConstant = -(value * 1000)
    network.disableEditMode()
  }
}

const setHtml = (html, metadata) => {
  if (Array.isArray(metadata)) {
    metadata?.forEach((data) => {
      let content = undefined

      if (Array.isArray(data.content)) {
        content = data.content.join(', ')
      } else {
        content = data.content
      }

      html.push(`<dt>${data.title}</dt><dd>${content}</dd>`)
    })
  } else {
    let content = undefined

    if (Array.isArray(metadata.content)) {
      content = metadata.content.join(', ')
    } else {
      content = metadata.content
    }

    html.push(`<dt>${metadata.title}</dt><dd>${content}</dd>`)
  }
}

const setIsLoading = (isLoading) => {
  const loadingElement = document.getElementById('graph-loading')

  if (isLoading) {
    loadingElement.style.display = 'block'
  } else {
    loadingElement.style.display = 'none'
  }
}

const setMetaData = (node) => {
  const metadata = document.getElementById('graph-metadata')
  const list = metadata.querySelector('.graph-metadata-list')
  const html = []

  list.innerHTML = ''

  if (node) {
    metadata.style.display = 'block'
    setHtml(html, node.metadata)
    list.innerHTML = html.join('')
  } else {
    metadata.style.display = ''
  }
}

const updateSliderValue = (type, value) => {
  const label = document.getElementById(`${type}-slider-label`)

  if (type === 'limit' && value === '1000') {
    label.textContent = 'kein Limit'
  } else {
    label.textContent = value
  }

  if (type === 'gravity') {
    setGravity(value)
  }
}
