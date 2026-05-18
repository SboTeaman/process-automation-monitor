# Process Automation Monitor - Frontend

Nowoczesny interfejs webowy dla systemu automatyzacji procesów.

## Uruchomienie

```bash
npm run dev
```

Aplikacja będzie dostępna na **http://localhost:3000**

## Budowanie

```bash
npm run build
```

Zbudowana aplikacja będzie w folderze `dist/`

## Konfiguracja

Ustaw API URL w `.env`:
```
VITE_API_URL=http://localhost:8080
```

## Struktura

```
src/
├── components/      # Komponenty (Layout, etc.)
├── pages/          # Strony (Login, Dashboard, Jobs, itd.)
├── store/          # Zustand state management
├── api/            # API client z Axios
├── App.tsx         # Main routing
└── index.css       # Tailwind CSS
```

## Funkcjonalności

- 🔐 **Autentykacja** - Login/Register z JWT tokens
- 📊 **Dashboard** - Overview systemu, wykresy
- ⚙️ **Zarządzanie zadaniami** - CRUD operacje na jobów
- 📈 **Analityka** - Statystyki, raporty
- 🚨 **Alerty** - Powiadomienia o błędach
- 📜 **Historia** - Logi wykonań

## Dependencje

- React 19 + TypeScript
- React Router v6 - routing
- Zustand - state management
- Recharts - wykresy
- Tailwind CSS - styling
- Axios - HTTP client
- date-fns - formatowanie dat
- lucide-react - ikony
