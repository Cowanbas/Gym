# Sobre
Aplicativo para guardar fichas de treino e marcar a constância dos dias treinados, com design minimalista e sem anúncios. Desenvolvi esse aplicativo durante os meus dias frequentando a academia. Anteriormente, eu estava utilizando o calendário, porém queria algo mais simples e dinâmico.

O aplicativo inclui a função de criar fichas de treino (com os exercícios de sua escolha) e recursos que achei úteis, como mudar o treino para o próximo dia, permitindo alterar a programação diária de forma muito simples.

O treino que você marca como executado no dia atual será registrado no calendário. O treino do dia será marcado e só poderá ser alterado na aba do calendário. Um exemplo: se, no treino de quinta-feira, você marcar que foi executado com a ficha A e depois alterar para a ficha B (cujos treinos são outros), mesmo com essa alteração, no calendário ficará registrado o da ficha A que você marcou originalmente.

Constância e gráficos: o aplicativo conta com um sistema de constância que é atualizado conforme o usuário vai marcando seus treinos concluídos. O sistema contabiliza os dias de segunda a domingo. Caso o usuário treine nos finais de semana, eles contam; se não for marcado nenhum treino nesses dias, eles são considerados dias de descanso e você não perderá a constância. O gráfico das últimas 16 semanas treinadas é atualizado de baixo para cima, começando na segunda e terminando no domingo, mostrando de forma geral quantos dias o usuário foi à academia ao longo das semanas.

O intuito do aplicativo é ser o mais simples e leve possível, para que qualquer pessoa possa utilizá-lo da maneira que quiser, seguindo uma base minimalista e prática. Ele foi pensado para os usuários que já sabem montar ou já têm uma ficha de treino, porém se cansaram de aplicativos de academia complicados.

A maneira mais justa que encontrei de monetizar o aplicativo foi adicionar um sistema de expansão de fichas: o usuário tem duas gratuitas, porém, se quiser adicionar mais do que isso, terá que pagar um valor simbólico de R$ 2,00.

# Features que serão adicionadas.

- *Opção de exercícios pre definidos dividido por grupos musculares para otimizar o processo de criação da ficha.* ❌
- *Nova aba para dividir as repetições com pesos diferentes.* ❌
- *Colocar idioma Português.* ❌

  

# Supostas próximas correções

- *Otimização mais ampla do código.* ❌ 
- *Corrigi tamanho do rodapé.* ❌


# Imagens


<p align="center">
  <img width="30%" alt="image" src="https://github.com/user-attachments/assets/1edf31be-32fb-4253-b281-7a3354278b5b" />
  <img width="30%" alt="image" src="https://github.com/user-attachments/assets/31266f66-d46e-4866-b305-ea47108b312b" />
  <img width="30%" alt="image" src="https://github.com/user-attachments/assets/77339ce6-62fb-4217-b605-60ed75cb6ecb" />
</p>

<p align="center">
  <img width="30%" alt="image" src="https://github.com/user-attachments/assets/928b83de-9e3d-4b6a-a783-7e4436bcc8f2" />
  <img width="30%" alt="image" src="https://github.com/user-attachments/assets/b8c312a4-9beb-4102-b13a-27852d35d660" />
  <img width="30%" alt="image" src="https://github.com/user-attachments/assets/67fc104f-d9cc-4629-a39d-db2cfd854779" />
</p>

```mermaid
graph TD
    classDef default fill:#e2e2e2,stroke:#333,stroke-width:1.5px,color:#000,font-family:sans-serif;

    A["<b>PASSO INICIAL: A ESCOLHA DO MODO</b><br/>Implementação de opção de criação<br/>Escolha se deseja fazer de forma MANUAL ou com PREDEFINIÇÕES."]
    
    B["<b>CAMINHO DA ESQUERDA (MANUAL)</b><br/>📄 Criação atual de templates<br/>O usuário monta a estrutura do zero de forma personalizada."]
    
    C["<b>CAMINHO DA DIREITA (PREDEFINIÇÕES)</b><br/>Escolha do Dia<br/>O usuário seleciona o dia da semana correspondente ao treino."]
    
    D["<b>[ Seleção dos Treinos do Dia ]</b><br/>Botões interativos para os grupos musculares: Peito | Ombro | Tríceps | Perna | Costas | Bíceps.<br/>Recurso: Botão para visualizar os exercícios de cada grupo antes de adicionar."]
    
    E["<b>[ Visualização dos Exercícios ]</b><br/>Ao clicar no grupo muscular, aparecem os nomes e os exercícios."]
    
    F["<b>[ Implementação na Ficha ]</b><br/>Ao escolher um exercício, ele é automaticamente implementado na ficha."]
    
    G["<b>[ Retorno e Flexibilidade ]</b><br/>O usuário pode voltar facilmente e escolher outro exercício de um novo grupo muscular para continuar montando o treino."]

    A -->|"2. RAMIFICAÇÃO DAS ESCOLHAS"| B
    A --> C
    C --> D
    D --> E
    E --> F
    F --> G
    G --> E










*Sua ficha, sua rotina, sua disciplina, sua liberdade!!*
