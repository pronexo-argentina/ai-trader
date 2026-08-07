# Backend v0.2

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn src.api:app --reload --port 8000
```

Luego abrí http://127.0.0.1:8000/docs o probá:

```bash
curl http://127.0.0.1:8000/health
```

La señal actual es análisis técnico por reglas, NO Machine Learning.
