#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os

# Python解释器的路径
py_dir = "C:\\Python310\\python.exe"

# 脚本的路径
script_dir = "D:\\Java\\Warp-exchange\\build\\bot\\bot.py"

# 打开100个命令提示符窗口，并在每个窗口中执行特定的脚本命令
for i in range(100):
    command = f'start cmd /k "{py_dir} {script_dir} --email=user{i}@example.com --password=password{i}"'
    os.system(command)