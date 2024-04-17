# ui

## Project setup
```
npm install
```

### Compiles and hot-reloads for development
```
export NODE_OPTIONS=--openssl-legacy-provider
npm run dev
```

### Compiles and minifies for production
```
npm run build
```

### Lints and fixes files
```
npm run lint
```

### Customize configuration
See [Configuration Reference](https://cli.vuejs.org/config/).

### Update package versions
Update minor versions:
```
ncu -u -t minor
```

Update to latest version (be aware of breaking changes):
```
ncu -u
```

Afterwards update the `package-lock.json`
```
npm update
```