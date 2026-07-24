# Linux terminal for Windows

## Install Ubuntu WSL

```powershell
wsl -l -v
wsl --install -d Ubuntu
wsl -d Ubuntu
```

## Enable Docker in WSL

Docker Desktop:

```text
Settings -> Resources -> WSL Integration -> enable Ubuntu -> Apply & Restart
```

Verify in Ubuntu:

```bash
docker compose version
```

If Docker permission is denied:

```bash
sudo usermod -aG docker $USER
exit
```

Then from PowerShell:

```powershell
wsl --shutdown
```

Open Ubuntu again.
