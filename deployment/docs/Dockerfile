# Production stage - Nginx
FROM nginx:alpine

# Copiar arquivos buildados (do contexto de build)
COPY ./ /usr/share/nginx/html/

# Copiar configuração nginx personalizada
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expor porta 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
