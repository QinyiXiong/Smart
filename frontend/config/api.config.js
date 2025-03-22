import {NODE_ENV} from '../environment'

const apisMap = {
  development: {
    FE: 'http://localhost:3000',
    BASE: 'http://localhost:8099/forest',
    CDN: '',
    PROXY: '/proxy',
    SOCKET: 'http://localhost:3000/ws',
    GRAVATAR: 'https://static.rymcu.com/avatar',
    VDITOR: 'https://static.rymcu.com/vditor@3.10.3',
    VDITOR_CSS: 'https://static.rymcu.com/vditor@3.10.3/dist/css/content-theme'
  },
  test: {
    FE: 'http://localhost:3000',
    BASE: 'http://localhost:8099/forest',
    CDN: '',
    PROXY: '/proxy',
    SOCKET: 'http://localhost:3000/ws',
    GRAVATAR: 'https://static.rymcu.com/avatar',
    VDITOR: 'https://static.rymcu.com/vditor@3.10.3',
    VDITOR_CSS: 'https://static.rymcu.com/vditor@3.10.3/dist/css/content-theme'
  },
  production: {
    FE: 'http://localhost:3000',
    BASE: 'http://localhost:8099/forest',
    CDN: '',
    PROXY: '/proxy',
    SOCKET: 'http://localhost:3000/ws',
    GRAVATAR: 'https://static.rymcu.com/avatar',
    VDITOR: 'https://static.rymcu.com/vditor@3.10.3',
    VDITOR_CSS: 'https://static.rymcu.com/vditor@3.10.3/dist/css/content-theme'
  }
}

export default apisMap[NODE_ENV]
