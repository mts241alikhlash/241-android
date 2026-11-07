#!/usr/bin/env node

import http from 'node:http'

const PORT = Number(process.env.PORT ?? 8090)

const ROUTES = [
  { prefix: '/auth', target: 'http://127.0.0.1:3001' },
  { prefix: '/me', target: 'http://127.0.0.1:4102' },
  { prefix: '/student-exams', target: 'http://127.0.0.1:4102' },
  { prefix: '/exam-schedules', target: 'http://127.0.0.1:4102' },
  { prefix: '/exam-sessions', target: 'http://127.0.0.1:4102' },
  { prefix: '/students', target: 'http://127.0.0.1:3901' },
]

function resolveTarget(pathname) {
  return ROUTES.find(
    (route) => pathname === route.prefix || pathname.startsWith(`${route.prefix}/`),
  )?.target
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url ?? '/', `http://${req.headers.host}`)
  const target = resolveTarget(url.pathname)

  if (!target) {
    res.writeHead(404, { 'content-type': 'application/json' })
    res.end(JSON.stringify({ statusCode: 404, message: 'No route', data: null }))
    return
  }

  const upstream = new URL(target)
  const proxied = http.request(
    {
      hostname: upstream.hostname,
      port: upstream.port,
      path: req.url,
      method: req.method,
      headers: { ...req.headers, host: upstream.host },
    },
    (upstreamRes) => {
      res.writeHead(upstreamRes.statusCode ?? 502, upstreamRes.headers)
      upstreamRes.pipe(res)
    },
  )

  proxied.on('error', (error) => {
    res.writeHead(502, { 'content-type': 'application/json' })
    res.end(
      JSON.stringify({ statusCode: 502, message: error.message, data: null }),
    )
  })

  req.pipe(proxied)
})

server.listen(PORT, '0.0.0.0', () => {
  console.log(`trial proxy on http://0.0.0.0:${PORT}`)
  for (const route of ROUTES) console.log(`  ${route.prefix} -> ${route.target}`)
})
