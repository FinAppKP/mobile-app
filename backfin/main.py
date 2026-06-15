# from fastapi import FastAPI, Depends, HTTPException, status
# from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
# from sqlalchemy.orm import Session
# from sqlalchemy import func
# from typing import List
# from jose import JWTError, jwt

# import models, schemas, security
# from database import engine, get_db
# import os

# # Создаем таблицы
# models.Base.metadata.create_all(bind=engine)

# app = FastAPI(title="Finance Management API")

# oauth2_scheme = OAuth2PasswordBearer(tokenUrl="login")

# def get_current_user(token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)):
#     credentials_exception = HTTPException(
#         status_code=status.HTTP_401_UNAUTHORIZED,
#         detail="Could not validate credentials",
#         headers={"WWW-Authenticate": "Bearer"},
#     )
#     try:
#         payload = jwt.decode(token, os.getenv("SECRET_KEY"), algorithms=[os.getenv("ALGORITHM")])
#         email: str = payload.get("sub")
#         if email is None:
#             raise credentials_exception
#     except JWTError:
#         raise credentials_exception
#     user = db.query(models.User).filter(models.User.email == email).first()
#     if user is None:
#         raise credentials_exception
#     return user

# # РОУТЫ: АВТОРИЗАЦИЯ И ПОЛЬЗОВАТЕЛИ

# @app.post("/register", response_model=schemas.UserResponse, tags=["Auth"])
# def register(user: schemas.UserCreate, db: Session = Depends(get_db)):
#     db_user = db.query(models.User).filter(models.User.email == user.email).first()
#     if db_user:
#         raise HTTPException(status_code=400, detail="Email already registered")
#     hashed_password = security.get_password_hash(user.password)
#     new_user = models.User(email=user.email, hashed_password=hashed_password)
#     db.add(new_user)
#     db.commit()
#     db.refresh(new_user)
#     return new_user

# @app.post("/login", response_model=schemas.Token, tags=["Auth"])
# def login(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
#     user = db.query(models.User).filter(models.User.email == form_data.username).first()
#     if not user or not security.verify_password(form_data.password, user.hashed_password):
#         raise HTTPException(status_code=400, detail="Incorrect email or password")
    
#     access_token = security.create_access_token(data={"sub": user.email})
#     return {"access_token": access_token, "token_type": "bearer"}

# # ==========================================
# # РОУТЫ: ТРАНЗАКЦИИ (Доходы и Расходы)
# # ==========================================
# @app.post("/transactions/", response_model=schemas.TransactionResponse, tags=["Transactions"])
# def create_transaction(transaction: schemas.TransactionCreate, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
#     new_tx = models.Transaction(**transaction.dict(), user_id=current_user.id)
#     db.add(new_tx)
#     db.commit()
#     db.refresh(new_tx)
#     return new_tx
# # --- Новые эндпоинты для аналитики и категорий ---

# @app.get("/categories/", tags=["Categories"])
# def get_categories(db: Session = Depends(get_db)):
#     # В идеале категории должны быть в базе, но для начала можно вернуть список
#     return [
#         {"id": 1, "name": "Продукты", "type": "expense"},
#         {"id": 2, "name": "Транспорт", "type": "expense"},
#         {"id": 3, "name": "Зарплата", "type": "income"},
#         {"id": 4, "name": "Жилье", "type": "expense"}
#     ]

# @app.get("/analytics/by-category", tags=["Analytics"])
# def get_analytics_by_category(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
#     # Группируем траты по категориям
#     results = db.query(
#         models.Category.name, 
#         func.sum(models.Transaction.amount)
#     ).join(models.Transaction).filter(
#         models.Transaction.user_id == current_user.id,
#         models.Transaction.type == "expense"
#     ).group_by(models.Category.name).all()
    
#     return {name: amount for name, amount in results}

# @app.get("/analytics/recommendations", tags=["Analytics"])
# def get_smart_recommendations(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
#     # Здесь имитация работы аналитического модуля
#     # В будущем сюда можно прикрутить модель на Pandas или Scikit-learn
#     return {
#         "user_id": current_user.id,
#         "insights": [
#             "Ваши расходы на категорию 'Транспорт' выросли на 20% в этом месяце.",
#             "Рекомендуем откладывать еще 2000 руб. в месяц, чтобы достичь цели 'Отпуск' вовремя.",
#             "Обнаружена подписка, которой вы редко пользуетесь. Проверьте раздел 'Развлечения'."
#         ],
#         "score": "Средний уровень финансовой грамотности"
#     }

# @app.get("/transactions/", response_model=List[schemas.TransactionResponse], tags=["Transactions"])
# def get_transactions(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
#     return db.query(models.Transaction).filter(models.Transaction.user_id == current_user.id).all()

# # ==========================================
# # РОУТЫ: ЦЕЛИ (Накопления)
# # ==========================================
# @app.post("/goals/", response_model=schemas.GoalResponse, tags=["Goals"])
# def create_goal(goal: schemas.GoalCreate, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
#     new_goal = models.Goal(**goal.dict(), user_id=current_user.id)
#     db.add(new_goal)
#     db.commit()
#     db.refresh(new_goal)
#     return new_goal

# @app.get("/goals/", response_model=List[schemas.GoalResponse], tags=["Goals"])
# def get_goals(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
#     return db.query(models.Goal).filter(models.Goal.user_id == current_user.id).all()


# # РОУТЫ: АНАЛИТИКА И РЕКОМЕНДАЦИИ
# @app.get("/analytics/summary", tags=["Analytics"])
# def get_analytics_summary(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
#     income = db.query(func.sum(models.Transaction.amount)).filter(
#         models.Transaction.user_id == current_user.id,
#         models.Transaction.type == "income"
#     ).scalar() or 0.0
    
#     expense = db.query(func.sum(models.Transaction.amount)).filter(
#         models.Transaction.user_id == current_user.id,
#         models.Transaction.type == "expense"
#     ).scalar() or 0.0

#     balance = income - expense
    
#     # Зачаток алгоритма рекомендаций (для диплома его можно будет усложнить через Pandas/Machine Learning)
#     recommendation = "Ваши финансы в норме."
#     if expense > income:
#         recommendation = "Внимание: ваши расходы превышают доходы! Рекомендуется проанализировать категории трат и сократить необязательные покупки."
#     elif expense > 0 and (expense / income) > 0.8:
#         recommendation = "Вы тратите более 80% своих доходов. Постарайтесь откладывать больше средств для достижения ваших финансовых целей."

#     return {
#         "total_income": income,
#         "total_expense": expense,
#         "current_balance": balance,
#         "recommendation": recommendation
#     }


from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from sqlalchemy import func, inspect, text
from typing import List
from jose import JWTError, jwt
import os

import models, schemas, security
from database import engine, get_db

# ----------------------------
# Создаем таблицы
# ----------------------------
models.Base.metadata.create_all(bind=engine)

app = FastAPI(title="Finance Management API")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="login")


DEFAULT_CATEGORIES = [
    {"name": "Коммунальные услуги", "type": "expense"},
    {"name": "Машина", "type": "expense"},
    {"name": "Продукты", "type": "expense"},
    {"name": "Передвижение", "type": "expense"},
    {"name": "Зарплата", "type": "income"},
]

DEFAULT_WALLETS = [
    {"key": "card", "title": "Безналичные", "icon_key": "default"},
    {"key": "cash", "title": "Наличные", "icon_key": "default"},
]


def ensure_schema():
    columns = {column["name"] for column in inspect(engine).get_columns("users")}
    if "seeded_categories" in columns:
        return

    default_value = "0" if engine.dialect.name == "sqlite" else "FALSE"
    with engine.begin() as connection:
        connection.execute(
            text(f"ALTER TABLE users ADD COLUMN seeded_categories BOOLEAN DEFAULT {default_value} NOT NULL")
        )


def seed_default_categories(db: Session, user: models.User):
    if user.seeded_categories:
        return

    existing_pairs = {
        (category.name, category.type)
        for category in db.query(models.Category).filter(models.Category.user_id == user.id).all()
    }
    for category in DEFAULT_CATEGORIES:
        pair = (category["name"], category["type"])
        if pair not in existing_pairs:
            db.add(models.Category(name=category["name"], type=category["type"], user_id=user.id))

    user.seeded_categories = True
    db.commit()
    db.refresh(user)


def seed_default_wallets(db: Session, user: models.User):
    existing_keys = {
        wallet.key
        for wallet in db.query(models.Wallet).filter(models.Wallet.user_id == user.id).all()
    }
    changed = False
    for wallet in DEFAULT_WALLETS:
        if wallet["key"] not in existing_keys:
            db.add(models.Wallet(
                key=wallet["key"],
                title=wallet["title"],
                icon_key=wallet["icon_key"],
                is_system=True,
                user_id=user.id
            ))
            changed = True
    if changed:
        db.commit()


ensure_schema()


# ----------------------------
# Утилита для получения текущего пользователя
# ----------------------------
def get_current_user(token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)):
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, os.getenv("SECRET_KEY"), algorithms=[os.getenv("ALGORITHM")])
        email: str = payload.get("sub")
        if email is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception
    user = db.query(models.User).filter(models.User.email == email).first()
    if user is None:
        raise credentials_exception
    return user


# ==========================================
# РОУТЫ: АВТОРИЗАЦИЯ И ПОЛЬЗОВАТЕЛИ
# ==========================================
@app.post("/register", response_model=schemas.UserResponse, tags=["Auth"])
def register(user: schemas.UserCreate, db: Session = Depends(get_db)):
    db_user = db.query(models.User).filter(models.User.email == user.email).first()
    if db_user:
        raise HTTPException(status_code=400, detail="Email already registered")
    hashed_password = security.get_password_hash(user.password)
    new_user = models.User(email=user.email, hashed_password=hashed_password)
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    return new_user


@app.post("/login", response_model=schemas.Token, tags=["Auth"])
def login(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == form_data.username).first()
    if not user or not security.verify_password(form_data.password, user.hashed_password):
        raise HTTPException(status_code=400, detail="Incorrect email or password")
    access_token = security.create_access_token(data={"sub": user.email})
    return {"access_token": access_token, "token_type": "bearer"}


# ==========================================
# РОУТЫ: КАТЕГОРИИ
# ==========================================
@app.get("/categories/", tags=["Categories"])
def get_categories(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    seed_default_categories(db, current_user)
    categories = db.query(models.Category).filter(models.Category.user_id == current_user.id).all()
    return [{"id": c.id, "name": c.name, "type": c.type} for c in categories]


@app.post("/categories/", tags=["Categories"])
def create_category(category: schemas.CategoryCreate, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    new_cat = models.Category(name=category.name, type=category.type, user_id=current_user.id)
    db.add(new_cat)
    db.commit()
    db.refresh(new_cat)
    return {"id": new_cat.id, "name": new_cat.name, "type": new_cat.type}


@app.put("/categories/{category_id}", tags=["Categories"])
def update_category(category_id: int, category: schemas.CategoryCreate, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    existing = db.query(models.Category).filter(
        models.Category.id == category_id,
        models.Category.user_id == current_user.id
    ).first()
    if existing is None:
        raise HTTPException(status_code=404, detail="Category not found")

    existing.name = category.name
    existing.type = category.type
    db.commit()
    db.refresh(existing)
    return {"id": existing.id, "name": existing.name, "type": existing.type}


@app.delete("/categories/{category_id}", tags=["Categories"])
def delete_category(category_id: int, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    existing = db.query(models.Category).filter(
        models.Category.id == category_id,
        models.Category.user_id == current_user.id
    ).first()
    if existing is None:
        raise HTTPException(status_code=404, detail="Category not found")

    db.query(models.Transaction).filter(
        models.Transaction.user_id == current_user.id,
        models.Transaction.category_id == category_id
    ).update({models.Transaction.category_id: None})
    db.delete(existing)
    db.commit()
    return {"ok": True}


# ==========================================
# РОУТЫ: КОШЕЛЬКИ
# ==========================================
@app.get("/wallets/", response_model=List[schemas.WalletResponse], tags=["Wallets"])
def get_wallets(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    seed_default_wallets(db, current_user)
    return db.query(models.Wallet).filter(
        models.Wallet.user_id == current_user.id
    ).order_by(models.Wallet.is_system.desc(), models.Wallet.id.asc()).all()


@app.post("/wallets/", response_model=schemas.WalletResponse, tags=["Wallets"])
def create_wallet(wallet: schemas.WalletCreate, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    title = wallet.title.strip()
    if not title:
        raise HTTPException(status_code=400, detail="Wallet title is required")

    new_wallet = models.Wallet(
        key="pending",
        title=title,
        icon_key=wallet.icon_key or "default",
        is_system=False,
        user_id=current_user.id
    )
    db.add(new_wallet)
    db.commit()
    db.refresh(new_wallet)
    new_wallet.key = f"wallet_{new_wallet.id}"
    db.commit()
    db.refresh(new_wallet)
    return new_wallet


@app.put("/wallets/{wallet_key}", response_model=schemas.WalletResponse, tags=["Wallets"])
def update_wallet(wallet_key: str, wallet: schemas.WalletUpdate, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    existing = db.query(models.Wallet).filter(
        models.Wallet.key == wallet_key,
        models.Wallet.user_id == current_user.id
    ).first()
    if existing is None:
        raise HTTPException(status_code=404, detail="Wallet not found")

    if wallet.title is not None and wallet.title.strip():
        existing.title = wallet.title.strip()
    if wallet.icon_key is not None and wallet.icon_key.strip():
        existing.icon_key = wallet.icon_key.strip()

    db.commit()
    db.refresh(existing)
    return existing


@app.delete("/wallets/{wallet_key}", tags=["Wallets"])
def delete_wallet(wallet_key: str, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    existing = db.query(models.Wallet).filter(
        models.Wallet.key == wallet_key,
        models.Wallet.user_id == current_user.id
    ).first()
    if existing is None:
        raise HTTPException(status_code=404, detail="Wallet not found")
    if existing.is_system:
        raise HTTPException(status_code=400, detail="System wallet cannot be deleted")

    db.delete(existing)
    db.commit()
    return {"ok": True}


# ==========================================
# РОУТЫ: ТРАНЗАКЦИИ
# ==========================================
@app.post("/transactions/", response_model=schemas.TransactionResponse, tags=["Transactions"])
def create_transaction(transaction: schemas.TransactionCreate, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    new_tx = models.Transaction(**transaction.dict(), user_id=current_user.id)
    db.add(new_tx)
    db.commit()
    db.refresh(new_tx)
    return new_tx


@app.get("/transactions/", response_model=List[schemas.TransactionResponse], tags=["Transactions"])
def get_transactions(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    return db.query(models.Transaction).filter(models.Transaction.user_id == current_user.id).order_by(models.Transaction.id.desc()).all()


@app.delete("/transactions/{transaction_id}", tags=["Transactions"])
def delete_transaction(transaction_id: int, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    transaction = db.query(models.Transaction).filter(
        models.Transaction.id == transaction_id,
        models.Transaction.user_id == current_user.id
    ).first()
    if transaction is None:
        raise HTTPException(status_code=404, detail="Transaction not found")

    if transaction.type == "goal" and transaction.description:
        goal = db.query(models.Goal).filter(
            models.Goal.user_id == current_user.id,
            models.Goal.title == transaction.description
        ).first()
        if goal is not None:
            goal.current_amount = max(0.0, goal.current_amount - transaction.amount)

    db.delete(transaction)
    db.commit()
    return {"ok": True}


# ==========================================
# РОУТЫ: АНАЛИТИКА
# ==========================================
@app.get("/analytics/by-category", tags=["Analytics"])
def get_analytics_by_category(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    results = db.query(
        models.Category.name,
        func.sum(models.Transaction.amount)
    ).join(models.Transaction).filter(
        models.Transaction.user_id == current_user.id,
        models.Transaction.type == "expense"
    ).group_by(models.Category.name).all()
    return {name: amount for name, amount in results}


@app.get("/analytics/recommendations", tags=["Analytics"])
def get_smart_recommendations(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    return {
        "user_id": current_user.id,
        "insights": [
            "Ваши расходы на категорию 'Транспорт' выросли на 20% в этом месяце.",
            "Рекомендуем откладывать еще 2000 руб. в месяц, чтобы достичь цели 'Отпуск' вовремя.",
            "Обнаружена подписка, которой вы редко пользуетесь. Проверьте раздел 'Развлечения'."
        ],
        "score": "Средний уровень финансовой грамотности"
    }


@app.get("/analytics/summary", tags=["Analytics"])
def get_analytics_summary(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    income = db.query(func.sum(models.Transaction.amount)).filter(
        models.Transaction.user_id == current_user.id,
        models.Transaction.type == "income"
    ).scalar() or 0.0

    expense = db.query(func.sum(models.Transaction.amount)).filter(
        models.Transaction.user_id == current_user.id,
        models.Transaction.type == "expense"
    ).scalar() or 0.0

    balance = income - expense
    recommendation = "Ваши финансы в норме."
    if expense > income:
        recommendation = "Внимание: ваши расходы превышают доходы! Рекомендуется проанализировать категории трат и сократить необязательные покупки."
    elif expense > 0 and (expense / income) > 0.8:
        recommendation = "Вы тратите более 80% своих доходов. Постарайтесь откладывать больше средств для достижения ваших финансовых целей."
    return {
        "total_income": income,
        "total_expense": expense,
        "current_balance": balance,
        "recommendation": recommendation
    }


# ==========================================
# РОУТЫ: ЦЕЛИ (Накопления)
# ==========================================
@app.post("/goals/", response_model=schemas.GoalResponse, tags=["Goals"])
def create_goal(goal: schemas.GoalCreate, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    new_goal = models.Goal(**goal.dict(), user_id=current_user.id)
    db.add(new_goal)
    db.commit()
    db.refresh(new_goal)
    return new_goal


@app.get("/goals/", response_model=List[schemas.GoalResponse], tags=["Goals"])
def get_goals(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    return db.query(models.Goal).filter(models.Goal.user_id == current_user.id).all()


@app.delete("/goals/{goal_id}", tags=["Goals"])
def delete_goal(goal_id: int, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    goal = db.query(models.Goal).filter(
        models.Goal.id == goal_id,
        models.Goal.user_id == current_user.id
    ).first()
    if goal is None:
        raise HTTPException(status_code=404, detail="Goal not found")

    db.query(models.Transaction).filter(
        models.Transaction.user_id == current_user.id,
        models.Transaction.type == "goal",
        models.Transaction.description == goal.title
    ).delete(synchronize_session=False)
    db.delete(goal)
    db.commit()
    return {"ok": True}


@app.post("/goals/{goal_id}/deposit", response_model=schemas.GoalResponse, tags=["Goals"])
def deposit_goal(goal_id: int, deposit: schemas.GoalDeposit, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    goal = db.query(models.Goal).filter(
        models.Goal.id == goal_id,
        models.Goal.user_id == current_user.id
    ).first()
    if goal is None:
        raise HTTPException(status_code=404, detail="Goal not found")

    goal.current_amount = min(goal.target_amount, goal.current_amount + deposit.amount)
    history_tx = models.Transaction(
        amount=deposit.amount,
        type="goal",
        description=goal.title,
        timestamp=deposit.timestamp or "",
        payment_method="goal",
        category_id=None,
        user_id=current_user.id
    )
    db.add(history_tx)
    db.commit()
    db.refresh(goal)
    return goal
