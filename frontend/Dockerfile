# Multi-stage build dla React + Vite

FROM node:20-alpine AS builder

WORKDIR /app

# Kopiowanie plików package
COPY package*.json ./

# Instalacja zależności
RUN npm ci --legacy-peer-deps

# Kopiowanie kodu
COPY . ./

# Budowanie (tylko Vite, bez TypeScript type checking)
RUN npx vite build

# Runtime image
FROM nginx:alpine

# Kopiowanie zbudowanej aplikacji
COPY --from=builder /app/dist /usr/share/nginx/html

# Tworzenie pliku konfiguracji Nginx
RUN cat > /etc/nginx/conf.d/default.conf << 'EOF'
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # Kompresja gzip
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # SPA routing - wszystkie ścieżki kierowane do index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache dla statycznych assetów
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
