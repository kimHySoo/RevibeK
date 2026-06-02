from fastapi import FastAPI

app = FastAPI()

# uvicorn app.main:app --reload
@app.get("/")
async def welcome() -> dict:
    return {
        "message": "Hello World!"
    }