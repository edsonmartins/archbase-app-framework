# Build stage - para o contexto de build com arquivos já compilados
FROM node:20-alpine

# Copiar arquivos buildados (do contexto de build)
COPY ./ /usr/share/nginx/html

# Production stage - Nginx
FROM nginx:alpine

# Copiar configuração nginx personalizada
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expor porta 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
