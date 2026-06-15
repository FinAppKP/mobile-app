from security import get_password_hash

try:
    p = "очень_длинный_пароль_" * 10
    print(f"Длина пароля до: {len(p)}")
    hashed = get_password_hash(p)
    print("Библиотека приняла пароль.")
    print(f"Хеш: {hashed}")
except Exception as e:
    print(f"Ошибка все еще тут: {e}")