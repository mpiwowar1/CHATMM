# ChatMM

ChatMM to szyfrowany komunikator end-to-end zbudowany na Spring Boot 4 (backend), React + TypeScript (frontend) oraz PostgreSQL. Wiadomości są szyfrowane po stronie klienta przy użyciu RSA i AES - serwer przechowuje wyłącznie zaszyfrowany ciphertext i nigdy nie ma dostępu do treści.

## Stos technologiczny

| Warstwa    | Technologia                          |
|------------|--------------------------------------|
| Backend    | Spring Boot 4, Java 17, Spring Security, STOMP/WebSocket |
| Frontend   | React 18, TypeScript, Vite, shadcn/ui |
| Baza danych | PostgreSQL 16                        |
| Konteneryzacja | Docker, Docker Compose           |

---

## Uruchamianie przez Docker

### Wymagania

- Docker >= 24
- Docker Compose >= 2.20

### Krok 1 - plik środowiskowy

Skopiuj przykładowy plik i uzupełnij wartości:

```bash
cp .env.example .env
```

Minimalne zmiany, które należy wykonać przed uruchomieniem:

```env
POSTGRES_PASSWORD=twoje_haslo
JWT_SECRET=wygenerowany_klucz_min_256_bitow
```

Klucz JWT można wygenerować poleceniem:

```bash
openssl rand -base64 64
```

### Krok 2 - uruchomienie

```bash
docker compose up --build
```

Po pierwszym uruchomieniu Docker pobierze obrazy bazowe, skompiluje backend Mavenem i zbuduje frontend Vitem. Może to potrwać kilka minut.

### Krok 3 - dostęp

| Usługa    | Adres                        |
|-----------|------------------------------|
| Frontend  | http://localhost:3000         |
| Backend   | http://localhost:8080/api/v1  |
| PostgreSQL | localhost:5432 (baza: `chatdb`) |

### Zatrzymanie

```bash
docker compose down
```

Aby usunąć również wolumen z danymi bazy:

```bash
docker compose down -v
```

---

## Zmienne środowiskowe

Pełna lista obsługiwanych zmiennych (`docker-compose.yml` + `.env`):

| Zmienna                    | Domyślna wartość                                         | Opis                                          |
|----------------------------|----------------------------------------------------------|-----------------------------------------------|
| `POSTGRES_PASSWORD`        | `chatmm_password_123`                                    | Hasło do bazy PostgreSQL                      |
| `JWT_SECRET`               | *(brak bezpiecznego domyślnego - ustaw własny)*          | Klucz podpisywania tokenów JWT (min. 256 bit) |
| `JWT_TOKEN_EXPIRATION`     | `900000`                                                 | Czas życia access tokena w ms (domyślnie 15 min) |
| `REFRESH_TOKEN_EXPIRATION` | `2592000000`                                             | Czas życia refresh tokena w ms (domyślnie 30 dni) |
| `ALLOWED_ORIGINS`          | `http://localhost:3000,http://localhost:5173`             | Dozwolone originy dla CORS                    |
| `DDL_AUTO`                 | `update`                                                 | Strategia Hibernate DDL                       |
| `VITE_API_URL`             | `http://localhost:8080/api/v1`                           | URL backendu widoczny dla frontendu           |

---

## API REST

Bazowy prefiks wszystkich endpointów: `/api/v1`

Endpointy spod `/auth/**` i `/ws/**` są publiczne. Wszystkie pozostałe wymagają nagłówka:

```
Authorization: Bearer <access_token>
```

---

### Autentykacja - `/auth`

#### `POST /auth/register`

Rejestracja nowego użytkownika. Klient generuje parę kluczy RSA po stronie przeglądarki i przesyła klucz publiczny oraz zaszyfrowany klucz prywatny.

Ciało żądania:

```json
{
  "email": "user@example.com",
  "name": "Jan Kowalski",
  "password": "haslo_zahashowane_na_frontendzie",
  "frontSalt": "base64_sol_do_hashowania_hasla",
  "publicKey": "base64_klucz_publiczny_rsa",
  "encryptedPrivateKey": "base64_zaszyfrowany_klucz_prywatny"
}
```

Odpowiedź: `201 Created` (brak ciała)

---

#### `GET /auth/salt?email=`

Pobranie solu użytkownika przed zalogowaniem - wymagane do ponownego zahashowania hasła po stronie klienta.

Odpowiedź:

```json
{
  "salt": "base64_sol"
}
```

---

#### `POST /auth/login`

Logowanie. Każde urządzenie rejestruje własny `deviceId`.

Ciało żądania:

```json
{
  "email": "user@example.com",
  "password": "zahashowane_haslo",
  "deviceId": "uuid-urzadzenia"
}
```

Odpowiedź:

```json
{
  "accessToken": "jwt...",
  "refreshToken": "uuid...",
  "id": 1,
  "email": "user@example.com",
  "name": "Jan Kowalski",
  "encryptedPrivateKey": "base64..."
}
```

---

#### `POST /auth/refresh`

Odświeżenie access tokena przy użyciu refresh tokena.

Ciało żądania:

```json
{
  "refreshToken": "uuid..."
}
```

Odpowiedź:

```json
{
  "accessToken": "nowy_jwt..."
}
```

---

#### `POST /auth/logout`

Unieważnienie sesji dla konkretnego urządzenia.

Ciało żądania:

```json
{
  "deviceId": "uuid-urzadzenia"
}
```

Odpowiedź: `204 No Content`

---

### Użytkownicy - `/users`

Wszystkie endpointy wymagają autoryzacji.

#### `GET /users/me`

Dane zalogowanego użytkownika.

Odpowiedź:

```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "Jan Kowalski",
  "publicKey": "base64..."
}
```

---

#### `GET /users/search?email=`

Wyszukanie użytkownika po dokładnym adresie email.

---

#### `GET /users/autocomplete?query=&limit=5`

Podpowiedzi użytkowników podczas wpisywania (domyślnie do 5 wyników).

---

### Konwersacje - `/conversations`

#### `POST /conversations`

Tworzenie nowej konwersacji. Pole `participantKeys` to mapa `userId -> zaszyfrowany_klucz_sesji_AES`. Klucz sesji jest szyfrowany kluczem publicznym RSA każdego uczestnika.

Ciało żądania:

```json
{
  "name": "Nazwa grupy (opcjonalnie)",
  "participantKeys": {
    "2": "base64_zaszyfrowany_klucz_sesji_dla_usera_2",
    "3": "base64_zaszyfrowany_klucz_sesji_dla_usera_3"
  }
}
```

Odpowiedź: `201 Created`

```json
{
  "id": 42
}
```

---

#### `GET /conversations`

Lista konwersacji zalogowanego użytkownika (stronicowanie).

Parametry query: `page` (domyślnie 0), `size` (domyślnie 10)

Odpowiedź: strona `Page<ConversationSummaryResponse>` ze standardowymi polami Spring Data.

---

#### `GET /conversations/{conversationId}/messages`

Historia wiadomości dla danej konwersacji (cursor pagination).

Parametry query:
- `cursor` - ID ostatniej wiadomości (opcjonalny, brak = od najnowszej)
- `limit` - liczba wiadomości na stronę (domyślnie 50)

Odpowiedź:

```json
{
  "messages": [
    {
      "id": 10,
      "senderId": 1,
      "senderName": "Jan Kowalski",
      "ciphertext": "base64...",
      "iv": "base64...",
      "timestamp": "2025-06-01T12:00:00"
    }
  ],
  "hasMore": true,
  "nextCursor": 9
}
```

---

#### `GET /conversations/{conversationId}/keys`

Klucze publiczne RSA wszystkich uczestników konwersacji - potrzebne do zaszyfrowania klucza sesji przy dodawaniu nowego uczestnika.

Odpowiedź:

```json
[
  {
    "userId": 2,
    "publicKey": "base64..."
  }
]
```

---

## WebSocket (STOMP)

Połączenie: `ws://localhost:8080/api/v1/ws`

Autentykacja odbywa się przez nagłówek STOMP `Authorization: Bearer <access_token>` podczas handshake CONNECT.

### Wysyłanie wiadomości

Cel: `/app/chat.send`

Payload:

```json
{
  "conversationId": 42,
  "ciphertext": "base64_zaszyfrowana_wiadomosc",
  "iv": "base64_wektor_inicjalizacyjny"
}
```

### Odbieranie wiadomości

Subskrypcja tematu konwersacji:

```
/topic/conversation.{conversationId}
```

Payload odpowiedzi jest identyczny z obiektem wiadomości z endpointu historii.

### Powiadomienia

Subskrypcja powiadomień o nowych wiadomościach (dla aktualnie zalogowanego użytkownika):

```
/user/queue/notifications
```

Subskrypcja powiadomień o nowych konwersacjach:

```
/user/queue/conversations
```

---

## Architektura szyfrowania

ChatMM stosuje szyfrowanie end-to-end oparte na modelu hybrydowym:

1. Przy rejestracji klient generuje parę kluczy RSA. Klucz publiczny trafia na serwer, klucz prywatny jest szyfrowany lokalnie (AES-GCM, klucz wyprowadzony z hasła przez PBKDF2) i przechowywany na serwerze w formie zaszyfrowanej.

2. Przy tworzeniu konwersacji klient generuje jednorazowy klucz sesji AES, szyfruje go kluczem publicznym RSA każdego uczestnika i wysyła wyniki do serwera.

3. Każda wiadomość jest szyfrowana kluczem sesji AES-GCM. Serwer przechowuje wyłącznie `ciphertext` i `iv`.

4. Hasło użytkownika jest hashowane po stronie klienta z użyciem solu (`frontSalt`) przed wysłaniem do serwera. Serwer hashuje otrzymaną wartość BCryptem i nie ma dostępu do oryginalnego hasła.

---

## Struktura projektu

```
CHATMM/
├── docker-compose.yml
├── .env.example
├── ChatMM-backend/          # Spring Boot 4
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/.../
│       │   ├── controller/  # AuthController, ConversationController, UserController, ChatController
│       │   ├── service/     # AuthService, ConversationService, ChatMessageService, NotificationService
│       │   ├── security/    # JwtAuthFilter, JwtService, StompAuthInterceptor
│       │   ├── model/       # User, Conversation, Message, Participant, RefreshToken
│       │   ├── dto/         # request/response DTOs
│       │   └── config/      # SecurityConfig, WebSocketConfig
│       └── test/            # testy jednostkowe i integracyjne
└── frontend/                # React + TypeScript + Vite
    ├── Dockerfile
    └── src/
        ├── components/
        ├── hooks/
        └── encryption/      # crypto.ts, messageCrypto.ts, deviceKeyStore.ts
```
