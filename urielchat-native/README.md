# Uriel Chat v0.6 — detecção de estabilidade experimental

Android nativo; compilável no GitHub Actions. O botão flutuante realiza duas capturas de tela pelo serviço de Acessibilidade, processa as duas imagens localmente com Google ML Kit Pose Detection e calcula a variação da posição dos olhos. Se estiver abaixo de 1,8% da menor dimensão da tela e a confiança for suficiente, tenta mover a câmera via gesto uma única vez e em seguida enviar um toque no local de disparo configurado pelo usuário.

## Limites honestos
- O sistema **não sabe se um personagem é inimigo ou aliado**. O detector de poses foi treinado para pessoas e pode não reconhecer avatares do Free Fire, especialmente longe.
- Comparar duas capturas não equivale a detectar movimento do personagem no mundo tridimensional; movimentação da câmera altera o resultado.
- Coordenadas de pixels não equivalem diretamente à orientação da câmera, portanto pode errar a direção e a magnitude.
- A captura pode ser bloqueada pelo Android ou pelo jogo; os gestos podem não funcionar.
- **Não detecta automaticamente quando o usuário atira**: funciona pelo botão flutuante e tenta disparar no local indicado após a análise. A captura pode adicionar latência.
- **Não implementa ESP, Chams ou acesso à memória/renderização do jogo**.
- Sem root, Shizuku sozinho não dá acesso ao estado interno do Free Fire; para ESP/Chams que enxergam atrás de paredes seria necessário suporte/integração não disponíveis.
- Não garante acertos nem funcionamento no jogo. Free Fire pode suspender contas pelo uso de ferramentas não autorizadas.

## Segurança
As imagens não são armazenadas ou transmitidas. A única rede é a autenticação KeyAuth. Nenhum segredo de administrador foi incorporado no APK. Não use como ferramenta de acessibilidade sem compreender as permissões. 
