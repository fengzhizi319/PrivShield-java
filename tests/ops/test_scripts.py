"""Ops 脚本语法与参数行为测试 / Operations Scripts Validation Tests.

验证 console/scripts/、scripts/dev/ 与 scripts/prod/ 目录下的运维脚本：
1. Bash 语法静态检查 (bash -n)
2. 脚本可执行权限与 Shebang 规范
3. 帮助参数 (-h / --help) 正常输出且退出码为 0
4. 环境变量与路径解析正确性
"""

from __future__ import annotations

import os
import subprocess
from pathlib import Path

import pytest

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
CONSOLE_SCRIPTS = PROJECT_ROOT / "console" / "scripts"
SCRIPTS_DEV = PROJECT_ROOT / "scripts" / "dev"
SCRIPTS_PROD = PROJECT_ROOT / "scripts" / "prod"


def _get_all_sh_scripts() -> list[Path]:
    """收集所有待测试的 shell 脚本。"""
    scripts: list[Path] = []
    if CONSOLE_SCRIPTS.exists():
        scripts.extend(CONSOLE_SCRIPTS.glob("*.sh"))
    if SCRIPTS_DEV.exists():
        scripts.extend(SCRIPTS_DEV.glob("*.sh"))
    if SCRIPTS_PROD.exists():
        scripts.extend(SCRIPTS_PROD.glob("*.sh"))
    return sorted(scripts)


@pytest.mark.parametrize("script_path", _get_all_sh_scripts(), ids=lambda p: str(p.relative_to(PROJECT_ROOT)))
def test_script_syntax_bash_n(script_path: Path) -> None:
    """验证所有 Shell 脚本符合 Bash 语法规范 (bash -n)。"""
    result = subprocess.run(
        ["bash", "-n", str(script_path)],
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, f"Syntax error in {script_path}:\n{result.stderr}"


@pytest.mark.parametrize("script_path", _get_all_sh_scripts(), ids=lambda p: str(p.relative_to(PROJECT_ROOT)))
def test_script_has_shebang_and_executable(script_path: Path) -> None:
    """验证脚本拥有合法的 Shebang 行及可执行权限。"""
    with script_path.open("r", encoding="utf-8", errors="ignore") as f:
        first_line = f.readline().strip()
    assert first_line.startswith("#!/usr/bin/env bash") or first_line.startswith("#!/bin/bash"), (
        f"{script_path} missing valid bash shebang"
    )
    assert os.access(script_path, os.X_OK), f"{script_path} is not executable (missing +x)"


def test_core_console_scripts_exist() -> None:
    """验证核心控制台脚本均存在。"""
    expected_scripts = [
        "dev-start-go.sh",
        "dev-start-all.sh",
        "dev-start.sh",
        "dev-start-go-mtls.sh",
        "dev-stop.sh",
        "prod-start-go.sh",
        "prod-start-all.sh",
        "prod-start.sh",
        "prod-start-go-mtls.sh",
        "prod-stop.sh",
        "docker-start-go.sh",
        "docker-start-python.sh",
        "docker-start-all.sh",
        "docker-stop.sh",
    ]
    for name in expected_scripts:
        script_file = CONSOLE_SCRIPTS / name
        assert script_file.exists(), f"Missing core console script: {name}"


def test_scripts_help_flags() -> None:
    """验证支持 --help 参数的脚本能正常输出使用帮助并返回 0。"""
    help_test_scripts = [
        SCRIPTS_DEV / "docker-start-agent.sh",
        SCRIPTS_DEV / "docker-start-llm.sh",
        SCRIPTS_PROD / "docker-start-agent.sh",
        SCRIPTS_PROD / "deploy-docker-compose.sh",
    ]
    for script in help_test_scripts:
        if not script.exists():
            continue
        res = subprocess.run(
            [str(script), "--help"],
            capture_output=True,
            text=True,
            check=False,
        )
        assert res.returncode == 0, f"{script} --help failed: {res.stderr}"
        assert "用法" in res.stdout or "Usage" in res.stdout or "options" in res.stdout.lower()
