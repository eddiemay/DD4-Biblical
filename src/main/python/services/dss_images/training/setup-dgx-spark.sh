# git clone https://github.com/eddiemay/DD4-Biblical
# cd DD4-Biblical/src/main/python/services/dss_images/training
sudo apt update
sudo apt install python3 python3-pip python3-venv
python3 --version
pip3 --version
python3 -m venv ~/detect
source ~/detect/bin/activate
python3 --version
pip3 --version
nvidia-smi
uname -a
pip install -r requirements.txt
pip install torch==2.9.0+cu130 torchvision==0.24.0 \
  --index-url https://download.pytorch.org/whl/cu130 \
  --extra-index-url https://pypi.org/simple
python -c "import torch; print(torch.__version__)"
python -c "import torch; print('CUDA:', torch.version.cuda); print('Available:', torch.cuda.is_available()); print('GPU:', torch.cuda.get_device_name(0)); print(torch.randn(3,3).cuda())"
pip install ninja
python -m pip install --no-build-isolation 'git+https://github.com/facebookresearch/detectron2.git'
python -c "import detectron2; print('Detectron2 imported successfully')"