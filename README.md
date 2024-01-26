# Trabalho Prático de Redes de Comunicação

Redes de Comunicação (CC3002), da Licenciatura em Ciência de Computadores (L:CC) na Faculdade de Ciências da Universidade do Porto (FCUP).  2023, 1o semestre.

[Enunciado do trabalho no Moodle](https://moodle2324.up.pt/mod/assign/view.php?id=63271)

## Entrega
Sábado, 06/Janeiro/2024.

## Grupo

202201926 Carla Henriques

202109728 Rui Santos

---
## Introdução

O trabalho consiste no desenvolvimento em Java de um servidor de chat e de um cliente simples para comunicar com ele. O servidor deve basear-se no modelo multiplex, aconselhando-se usar como ponto de partida o programa desenvolvido na ficha de exercícios nº 5 das aulas práticas. Quanto ao cliente, deve partir deste esqueleto, que implementa uma interface gráfica simples, e completá-lo com a implementação do lado cliente do protocolo. O cliente deve usar duas threads, de modo a poder receber mensagens do servidor enquanto espera que o utilizador escreva a próxima mensagem ou comando (caso contrário bloquearia na leitura da socket, tornando a interface inoperacional).

## Comandos disponíveis 
Para verficar uma descrição mais completa , ver assignment.pdf
>/nick 

>/join 

>/leave

>/bye 

## Utilização 
Seguir os seguintes passos:

1) Fazer o make para compilar os ficheiros .java 

2) Num terminal :  java ChatServer 8000 

3) Noutro(s) terminal(ais) :  java ChatClient.java localhost 8000


