# 🛒 Lista Fácil

Um aplicativo Android nativo para gerenciamento de listas de compras, com recurso de **inteligência artificial (OCR)** para escanear listas manuscritas ou impressas diretamente pela câmera.
Com a lista no smartphone, você pode ir marcando os itens que colocou no carrinho enquanto faz suas compras. Simples e útil.

## 📱 Funcionalidades

* **Lista Interativa:** Adicione, marque e exclua itens com facilidade.
* **Scanner Inteligente:** Use a câmera para fotografar uma lista de papel, quadro ou lousa.
* **Recorte (Crop):** Interface para recortar a imagem antes do processamento, melhorando a precisão.
* **OCR Offline:** Utiliza o Google ML Kit para reconhecer texto sem precisar de internet.
* **Smart Auto-Corrector:** Algoritmo personalizado (Distância de Levenshtein) que corrige erros de leitura (ex: entende que "Abaçat" é "Abacate") baseado em um dicionário de itens comuns no Brasil.

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** Kotlin
* **Interface:** Jetpack Compose (Material Design 3)
* **ML & IA:** Google ML Kit (Text Recognition v2)
* **Câmera & Imagem:** CameraX + CanHub Android-Image-Cropper
* **Arquitetura:** MVVM (Model-View-ViewModel) concept

## 📸 Screenshots

| Lista Vazia | Escaneando (Crop) | Itens Reconhecidos |
|:-----------:|:-----------------:|:------------------:|
| ![img-02-lista_vazia](https://github.com/user-attachments/assets/abe38cf2-de23-4cc4-994f-fa563a974b6e) | ![img-05-crop](https://github.com/user-attachments/assets/6277bc21-376d-40f5-94cb-aaf263f1cc4d) | ![img-07-seleção](https://github.com/user-attachments/assets/2d9c4f61-e749-4792-b953-98ee5af86bf9) |

## 🚀 Como rodar o projeto

1.  Clone este repositório.
2.  Abra no Android Studio (Ladybug ou superior).
3.  Aguarde o Sync do Gradle.
4.  Execute em um dispositivo físico (recomendado para testar a câmera) ou emulador.

## 👨‍💻 Autor

Desenvolvido por **mendes79**.
