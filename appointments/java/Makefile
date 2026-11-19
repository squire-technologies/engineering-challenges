include .env
export

TOOL = ./mvnw -q test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.squire.salon.tools

.PHONY: setup up seed dev check-bookings down

setup:
	./mvnw -DskipTests package

up:
	docker compose up -d --wait

seed: up
	$(TOOL).Seed

dev: up
	./mvnw -q spring-boot:run

check-bookings:
	$(TOOL).CheckBookings

down:
	docker compose down
