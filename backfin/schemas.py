from pydantic import BaseModel, EmailStr
from datetime import date
from typing import Optional


class UserCreate(BaseModel):
    email: EmailStr
    password: str


class UserResponse(BaseModel):
    id: int
    email: str

    class Config:
        orm_mode = True


class Token(BaseModel):
    access_token: str
    token_type: str


class CategoryBase(BaseModel):
    name: str
    type: str


class CategoryCreate(CategoryBase):
    pass


class CategoryResponse(CategoryBase):
    id: int

    class Config:
        orm_mode = True


class WalletCreate(BaseModel):
    title: str
    icon_key: Optional[str] = "default"


class WalletUpdate(BaseModel):
    title: Optional[str] = None
    icon_key: Optional[str] = None


class WalletResponse(BaseModel):
    id: int
    key: str
    title: str
    icon_key: str
    is_system: bool

    class Config:
        orm_mode = True


class TransactionBase(BaseModel):
    amount: float
    type: str
    payment_method: Optional[str] = ""
    timestamp: str
    category_id: Optional[int] = None
    description: Optional[str] = None


class TransactionCreate(TransactionBase):
    pass


class TransactionResponse(TransactionBase):
    id: int
    user_id: int

    class Config:
        orm_mode = True


class GoalCreate(BaseModel):
    title: str
    target_amount: float
    deadline: date


class GoalDeposit(BaseModel):
    amount: float
    timestamp: Optional[str] = None


class GoalResponse(GoalCreate):
    id: int
    current_amount: float

    class Config:
        orm_mode = True
