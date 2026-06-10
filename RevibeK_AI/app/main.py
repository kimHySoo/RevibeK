from fastapi import FastAPI

from app.routers import analyze

app = FastAPI(title="RevibeK AI Server")

# 라우터 등록
app.include_router(analyze.router)

# uvicorn app.main:app --reload
@app.get("/")
async def welcome() -> dict:
    return {
        "message": "Hello World!"
    }