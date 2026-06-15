import os
from sqlalchemy import create_engine
from dotenv import load_dotenv

load_dotenv()
url = os.getenv("DATABASE_URL")

try:
    engine = create_engine(url)
    with engine.connect() as connection:
        print(" УСПЕХ: Бэкенд видит базу данных!")
        from models import Base
        Base.metadata.create_all(bind=engine)
        print(" ТАБЛИЦЫ СОЗДАНЫ: Проверь pgAdmin (нажми Refresh на папке Tables).")
except Exception as e:
    print(" ОШИБКА ПОДКЛЮЧЕНИЯ:")
    print(e)