.PHONY: setup up seed dev race down

setup:
	npm install
	npx prisma generate

up:
	docker compose up -d --wait

seed: up
	npx prisma migrate deploy
	npx prisma generate
	npx prisma db seed

dev: up
	npm run dev

race:
	npm run race

down:
	docker compose down
