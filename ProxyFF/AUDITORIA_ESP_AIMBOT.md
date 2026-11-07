# Auditoria de viabilidade — ESP e assistência de mira

Data: 2026-11-07. Escopo: análise de evidências, não implementação comprovada no Free Fire.

## Evidências já disponíveis

- FFH4X PROXY.apk: pacote `com.edsonmodz.vpn`, versão `0.5-final43`.
- Free Fire MAX_2.132.1.apks: pacote `com.dts.freefiremax`, cliente com componentes Unity/IL2CPP.
- `libedson_projection.so`: símbolos JNI `EspProjection_nativePoint`, `nativeBox`, `nativeBoxes`.
- `libedson_collider.so`: símbolos JNI `ColliderNative_info`, `profileId`, `generate`, `edit`, `EdsonNative_transform`.
- FFH4X inclui VPN/SOCKS5 e textos de interface AIMBOT/ESP, mas também `game_acceptance=unknown`, `efeito_no_jogo=nao_comprovado` e `server acceptance pending`.
- Não foi identificada evidência suficiente de leitura de posições reais, matriz da câmera, evento de disparo ou confirmação de headshot.
- Cliente Netflix X Proxy FF: VPN Android/SOCKS5 compilada; não contém ESP/Aimbot funcional.

## Matriz de bloqueios

| Capacidade | Evidência necessária | Situação |
|---|---|---|
| Dados de personagens | Fonte autorizada de identificadores e coordenadas reais, observável em execução | Ausente |
| Cabeça / ossos | Transformações e referência de colisão, associadas a personagem e tempo | Ausente |
| Projeção ESP | Matriz de câmera + coordenadas sincronizadas + comparação com ground truth | Apenas símbolos de biblioteca |
| Disparo | Evento efetivo e interface de teste para assistência | Ausente |
| Resultado do disparo | Registro do mecanismo de colisão/autoridade em ambiente controlado | Ausente |
| VPN | Servidor SOCKS5 real, teste de TCP/UDP/DNS e aparelho Android | Pendente |

## Critérios para declarar sucesso

**ESP:** dados de referência com origem conhecida; linhas acompanham personagens móveis, com erro de projeção medido.

**Assistência:** evento de disparo verificado; ação somente durante o disparo; resultado confirmado por sistema de colisão em jogo de teste autorizado.

**VPN:** conexão real TCP/UDP através de endpoint SOCKS5, com logs de encaminhamento, DNS e latência.

## Próximas evidências úteis, sem reenviar APKs

1. Código-fonte ou documentação autorizada das bibliotecas FFH4X, especialmente contrato de entradas e saídas das funções JNI.
2. Demonstração contínua com logs e dados de referência, caso exista, das funções originais.
3. Ambiente de teste próprio/autorizado com posições dos personagens, matrizes da câmera e eventos de disparo observáveis.
4. Modelo/versão exata do Android e logs de instalação/execução do cliente VPN.
5. Endpoint SOCKS5 administrado pelo usuário para testar a rede; não registrar credenciais neste repositório.

## Limites do que foi demonstrado

Nomes de funções e textos de interface não provam comportamento no jogo. SOCKS5 encaminha rede, não fornece memória de outro processo. A compilação do APK não equivale a teste em dispositivo. Não declarar ESP, Aimbot, headshot ou antiban como funcionais sem validação experimental.
