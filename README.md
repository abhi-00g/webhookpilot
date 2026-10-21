# Smart Finance Tracker

A full-stack personal finance application with JWT authentication, expense tracking, budget management, AI-powered financial insights, and recurring expense detection.

## Architecture

```
smart-finance-tracker/
├── backend/                  # Express.js REST API
│   ├── controllers/          # Route handlers (auth, expense, budget, AI, export)
│   ├── models/               # Sequelize ORM models (User, Expense, Budget)
│   ├── routes/               # Express route definitions
│   ├── middlewares/           # JWT authentication middleware
│   ├── services/             # Business logic (recurring expense detection)
│   ├── tests/                # Jest + Supertest test suite (22 tests)
│   └── server.js             # App entry point with CORS, health check
├── frontend/                 # React SPA
│   └── src/
│       ├── pages/            # Dashboard, Expenses, Login, Register
│       ├── components/       # ExpenseCard, Navbar
│       └── styles/           # Component CSS
├── docker-compose.yml        # Backend + PostgreSQL containers
└── .github/workflows/ci.yml  # GitHub Actions CI
```

## Tech Stack

**Backend:** Node.js, Express 5, Sequelize ORM, PostgreSQL, JWT, bcrypt, Cohere API

**Frontend:** React 19, React Router v7, Axios

**Testing:** Jest, Supertest (22 backend tests across auth, expenses, budgets)

**DevOps:** Docker Compose, GitHub Actions CI

## Features

- **Authentication** — Register, login, and password update with bcrypt hashing and JWT tokens. Password validation enforces uppercase, number, special character, and minimum length.

- **Expense Tracking** — Create, list, and delete expenses with category, amount, date, and description. All amounts in USD.

- **Budget Management** — Set per-category spending limits with both predefined categories (groceries, rent, utilities, etc.) and custom categories. Budget responses include current spending and remaining balance.

- **Recurring Expense Detection** — Analyzes expense history to identify monthly recurring patterns using date-interval matching with a 5-day tolerance window.

- **AI Financial Insights** — Sends expense and budget data to Cohere's command-r-plus model for personalized saving suggestions.

- **Data Export** — CSV export for both expenses and budgets.

- **Dashboard** — Summary view with total spending, top categories, and detected subscriptions.

## Running Locally

### Prerequisites

- Node.js 20+
- PostgreSQL 16+

### Backend

```bash
cd backend
cp .env.example .env
# Edit .env with your PostgreSQL credentials and Cohere API key
npm install
npm run dev
```

The backend runs on `http://localhost:3000`. The database tables are created automatically via `sequelize.sync()` on startup.

### Frontend

```bash
cd frontend
npm install
npm start
```

The frontend runs on `http://localhost:3001` and proxies API requests to the backend via the `proxy` field in `package.json`.

### Docker Compose

```bash
docker compose up
```

Starts the backend and PostgreSQL. The backend is available on port 3000. Set `COHERE_API_KEY` in your environment for the AI insights feature.

## Running Tests

```bash
cd backend
npm test
```

Runs 22 tests across 3 suites:

- **auth.test.js** — Register (success, duplicate email, weak password, mismatched passwords, missing fields), login (success, wrong password, nonexistent user)
- **expense.test.js** — Create expense (success, negative amount, missing fields, unauthenticated), list expenses, delete expense (success, nonexistent)
- **budget.test.js** — Create budget (predefined category, custom category, duplicate, negative limit), list budgets with spending, update limit, delete budget

Tests run against a real PostgreSQL instance (not mocks) using `sequelize.sync({ force: true })` to reset the database before each suite.

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Register a new user |
| POST | `/api/auth/login` | No | Login, returns JWT |
| PATCH | `/api/auth/update-password` | No | Update password by email |
| GET | `/api/expenses` | Yes | List user's expenses |
| POST | `/api/expenses` | Yes | Create an expense |
| DELETE | `/api/expenses/:id` | Yes | Delete an expense |
| GET | `/api/expenses/recurring` | Yes | Detect recurring expenses |
| GET | `/api/budgets` | Yes | List budgets with spending |
| POST | `/api/budgets` | Yes | Create a budget |
| PATCH | `/api/budgets/:id` | Yes | Update budget limit |
| DELETE | `/api/budgets/:id` | Yes | Delete a budget |
| GET | `/api/summary/dashboard` | Yes | Dashboard summary |
| GET | `/api/summary/pie` | Yes | Category breakdown |
| GET | `/api/ai/insights` | Yes | AI financial suggestions |
| GET | `/api/export/expenses` | Yes | CSV export of expenses |
| GET | `/api/export/budgets` | Yes | CSV export of budgets |
| GET | `/api/health` | No | Health check |
