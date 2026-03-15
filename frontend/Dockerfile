FROM node:20-alpine AS base
WORKDIR /app

FROM base AS deps
WORKDIR /app
COPY package.json ./
COPY package-lock.json* ./
RUN npm install

FROM base AS dev
COPY package.json ./
COPY package-lock.json* ./
RUN npm install
COPY . .
EXPOSE 3000
ENV PORT=3000
CMD ["npm", "run", "dev", "--", "--hostname", "0.0.0.0", "--port", "3000"]

FROM base AS builder
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM base
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
EXPOSE 3000
ENV PORT=3000
CMD ["node", "server.js"]
