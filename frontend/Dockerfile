# =============================================================================
# Frontend — Next.js
# =============================================================================

# --- Etapa 1: dependencias ----------------------------------------------------
FROM node:22-alpine AS deps
WORKDIR /origen
COPY package.json package-lock.json ./
# 'ci' y no 'install': instala exactamente lo del lock. Un despliegue no puede
# traer versiones distintas de las que se probaron.
RUN npm ci

# --- Etapa 2: compilar --------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /origen
COPY --from=deps /origen/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# --- Etapa 3: ejecutar --------------------------------------------------------
FROM node:22-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

RUN addgroup -S nodejs && adduser -S nextjs -G nodejs

# La salida 'standalone' trae solo lo que la aplicacion usa de verdad.
COPY --from=build --chown=nextjs:nodejs /origen/.next/standalone ./
COPY --from=build --chown=nextjs:nodejs /origen/.next/static ./.next/static
COPY --from=build --chown=nextjs:nodejs /origen/public ./public

USER nextjs
EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

CMD ["node", "server.js"]
