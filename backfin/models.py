from sqlalchemy import Boolean, Column, Integer, String, Float, ForeignKey, Date, Enum
from sqlalchemy.orm import relationship
from database import Base
import enum

class TransactionType(str, enum.Enum):
    income = "income"
    expense = "expense"

class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True)
    hashed_password = Column(String)
    seeded_categories = Column(Boolean, default=False, nullable=False)
    
    transactions = relationship("Transaction", back_populates="owner")
    goals = relationship("Goal", back_populates="owner")
    wallets = relationship("Wallet", back_populates="owner")

class Category(Base):
    __tablename__ = "categories"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, index=True)
    type = Column(String)  # income / expense

    user_id = Column(Integer, ForeignKey("users.id"), nullable=True)


class Wallet(Base):
    __tablename__ = "wallets"

    id = Column(Integer, primary_key=True, index=True)
    key = Column(String, index=True)
    title = Column(String)
    icon_key = Column(String, default="default")
    is_system = Column(Boolean, default=False, nullable=False)

    user_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="wallets")

class Transaction(Base):
    __tablename__ = "transactions"
    id = Column(Integer, primary_key=True, index=True)
    amount = Column(Float)
    type = Column(String) # income / expense / cash / transfer
    description = Column(String, nullable=True)
    timestamp = Column(String)
    payment_method = Column(String, nullable=True)
    
    category_id = Column(Integer, ForeignKey("categories.id"))
    user_id = Column(Integer, ForeignKey("users.id"))
    
    owner = relationship("User", back_populates="transactions")


class Goal(Base):
    __tablename__ = "goals"
    id = Column(Integer, primary_key=True, index=True)
    title = Column(String)
    target_amount = Column(Float)
    current_amount = Column(Float, default=0.0)
    deadline = Column(Date)
    
    user_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="goals")
