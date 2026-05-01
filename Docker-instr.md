##ChatMM
---

## Wymagania

- **Docker Desktop** (Windows/Mac) lub **Docker Engine** (Linux)
- **Docker Compose** v2.0+
- **4GB RAM** minimum dla kontenerów
- **Wolne porty:** 3000, 8080, 5432

### Sprawdź czy masz Docker:
docker --version
docker compose version
---

## Szybki start z Docker

### 1. Sklonuj repozytorium

```bash
git clone https://github.com/mpiwowar1/CHATMM.git
cd CHATMM
```

### 2. Sklonuj branche backend i frontend
Projekt używa struktury z oddzielnymi branchami:

```bash
# Sklonuj backend
git clone -b backend https://github.com/mpiwowar1/CHATMM.git backend

# Sklonuj frontend
git clone -b frontend https://github.com/mpiwowar1/CHATMM.git frontend
```

Struktura katalogów powinna wyglądać tak:
```
CHATMM/
├── docker-compose.yml
├── .env.example
├── README.md
├── backend/
│   └── ChatMM-backend/
│       ├── Dockerfile
│       └── src/
└── frontend/
    └── frontend/
        ├── Dockerfile
        └── src/
```

### 3. Skonfiguruj zmienne środowiskowe 

```bash
# Skopiuj przykładowy plik .env
cp .env.example .env

# Edytuj .env i ustaw własne wartości

**WAŻNE:** Zmień przynajmniej te wartości:
- `JWT_SECRET` - wygeneruj silny klucz (min. 64 znaki)
- `POSTGRES_PASSWORD` - ustaw bezpieczne hasło

**Wygeneruj JWT Secret:**
```bash
# Linux/Mac
openssl rand -base64 64

# Windows (PowerShell)
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))
```

### 4. Uruchom aplikację

```bash
# Zbuduj i uruchom wszystkie kontenery
docker compose up --build -d

# Zobacz logi (opcjonalnie)
docker compose logs -f
```

**Pierwszy build zajmie ~5-10 minut** (Maven pobiera zależności, npm instaluje pakiety).

### 5. Sprawdź status

```bash
docker compose ps
```

Wszystkie serwisy powinny mieć status `Up (healthy)`: aktualnie backend i frontend sa unhealthy, musze zmienic health checki
``` 
NAME              STATUS
chatmm-backend    Up (healthy) 
chatmm-frontend   Up (healthy)
chatmm-postgres   Up (healthy)
```

---

## 🌐 Dostęp do aplikacji

Po pomyślnym uruchomieniu:

- **Frontend (UI):** http://localhost:3000
- **Backend (API):** http://localhost:8080/api/v1
- **PostgreSQL:** localhost:5432

### Pierwsze użycie:

1. Otwórz http://localhost:3000
2. Kliknij "Register" i utwórz konto
3. Zaloguj się
4. Rozpocznij chatowanie! 🎉

---

## ⚙️ Konfiguracja

### Plik `.env`

Wszystkie konfiguracje znajdują się w pliku `.env`:

```env
# Database
POSTGRES_PASSWORD=twoje_haslo

# JWT Security
JWT_SECRET=twoj_bardzo_dlug_i_bezpieczny_klucz_min_64_znaki
JWT_TOKEN_EXPIRATION=900000          # 15 minut
REFRESH_TOKEN_EXPIRATION=2592000000  # 30 dni

# CORS
ALLOWED_ORIGINS=http://localhost:3000

# Database DDL (update | validate | create-drop)
DDL_AUTO=update

# Frontend API URL
VITE_API_URL=http://localhost:8080/api/v1
```

### Zmiana portów

Jeśli porty są zajęte, edytuj `docker-compose.yml`:

```yaml
services:
  frontend:
    ports:
      - "3001:80"  # zamiast 3000:80
  
  backend:
    ports:
      - "8081:8080"  # zamiast 8080:8080
```

---

## 🛠️ Zarządzanie

### Podstawowe komendy:

```bash
# Uruchom kontenery
docker compose up -d

# Zatrzymaj kontenery
docker compose down

# Restart wszystkich serwisów
docker compose restart

# Restart konkretnego serwisu
docker compose restart backend

# Zobacz logi
docker compose logs -f

# Zobacz logi konkretnego serwisu
docker compose logs -f backend
```

### Rebuild po zmianach w kodzie:

```bash
# Przebuduj i uruchom
docker compose up --build -d

# Lub rebuild bez cache
docker compose build --no-cache
docker compose up -d
```

### Czyszczenie:

```bash
# Stop i usuń kontenery (zachowuje dane)
docker compose down

# Stop i usuń kontenery + volumes (USUWA BAZĘ DANYCH!)
docker compose down -v

# Usuń nieużywane obrazy i cache
docker system prune -a
```

---

## 🗄️ Baza danych

### Połączenie z PostgreSQL:

```bash
# Przez Docker
docker compose exec postgres psql -U postgres -d chatdb

# Lub bezpośrednio
psql -h localhost -p 5432 -U postgres -d chatdb
```

### Backup i Restore:

```bash
# Backup
docker compose exec postgres pg_dump -U postgres chatdb > backup.sql

# Restore
docker compose exec -T postgres psql -U postgres chatdb < backup.sql
```

---

## 🆘 Troubleshooting

### Frontend nie ładuje się

```bash
# Sprawdź logi
docker compose logs frontend

# Sprawdź czy kontener działa
docker compose ps

# Restart frontendu
docker compose restart frontend
```

### Backend nie odpowiada

```bash
# Sprawdź logi
docker compose logs backend

# Sprawdź health endpoint
curl http://localhost:8080/api/v1/actuator/health

# Sprawdź połączenie z bazą
docker compose logs postgres
```

### Port już zajęty

```bash
# Windows - sprawdź co używa portu
netstat -ano | findstr :3000

# Linux/Mac
lsof -i :3000

# Zmień port w docker-compose.yml
```

### Frontend restartuje się w kółko

```bash
# Zobacz logi
docker compose logs frontend

# Przebuduj bez cache
docker compose down
docker rmi chatmm-frontend
docker compose build --no-cache frontend
docker compose up -d
```

### Build failuje

```bash
# Wyczyść cache Dockera
docker builder prune -f

# Rebuild bez cache
docker compose build --no-cache
docker compose up -d
```

### Brak połączenia backend ↔ PostgreSQL

```bash
# Sprawdź czy PostgreSQL jest healthy
docker compose ps postgres

# Sprawdź logi PostgreSQL
docker compose logs postgres

# Zrestartuj oba serwisy
docker compose restart postgres backend
```

### Ogólne problemy

```bash
# Wyczyść wszystko i zacznij od nowa
docker compose down -v
docker system prune -a
docker compose up --build -d
```

---

## 📊 Monitoring

### Sprawdź użycie zasobów:

```bash
docker stats
```

### Health checks:

```bash
# Backend
curl http://localhost:8080/api/v1/actuator/health

# Frontend
curl http://localhost:3000

# PostgreSQL
docker compose exec postgres pg_isready -U postgres
```

---

## 🏗️ Architektura

### Stack technologiczny:

**Backend:**
- Spring Boot 4.0.4
- Java 17
- PostgreSQL 16
- WebSocket (STOMP)
- Spring Security + JWT
- Maven

**Frontend:**
- React 19
- Vite
- TypeScript
- Tailwind CSS
- shadcn/ui

**Deployment:**
- Docker
- Docker Compose
- Nginx (dla frontendu)
- Multi-stage builds

### Struktura projektu:

```
├── main branch           # Orkiestracja Docker
│   ├── docker-compose.yml
│   ├── .env.example
│   └── README.md
├── backend branch        # Spring Boot backend
│   └── ChatMM-backend/
│       ├── Dockerfile
│       └── src/
└── frontend branch       # React frontend
    └── frontend/
        ├── Dockerfile
        └── src/
```

---

## 🔒 Bezpieczeństwo

### Przed wdrożeniem produkcyjnym:

- [ ] Zmień `JWT_SECRET` na losowy 64+ znakowy klucz
- [ ] Zmień `POSTGRES_PASSWORD`
- [ ] Ustaw `DDL_AUTO=validate` (nie `update`)
- [ ] Skonfiguruj HTTPS/SSL
- [ ] Ogranicz `ALLOWED_ORIGINS` do swojej domeny
- [ ] Włącz automatyczne backupy bazy danych
- [ ] Skonfiguruj monitoring (Prometheus/Grafana)
- [ ] Użyj Docker secrets zamiast .env dla produkcji

---


