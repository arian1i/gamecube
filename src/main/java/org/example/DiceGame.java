package org.example;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class DiceGame extends JFrame {
    private static final int WINNING_SCORE = 1000; // Очки для победы
    private static final int OPENING_SCORE = 75;    // Минимальная сумма очков для открытия
    private static final int DICE_COUNT = 5;         // Количество кубиков
    private static final int ANIMATION_FRAMES = 30;  // Количество кадров анимации (3 секунды)

    private Image backgroundImage;                   // Поле для хранения изображения фона

    private static class Player {
        String name;
        int totalScore;      // Итоговый счет
        int currentScore;    // Очки за текущий ход
        boolean isGameOpened; // Флаг, открыта ли игра игроком

        Player(String name) {
            this.name = name;
            this.totalScore = 0;
            this.currentScore = 0;
            this.isGameOpened = false;
        }
    }

    private ArrayList<Player> players;              // Список игроков
    private int currentPlayerIndex;                 // Индекс текущего игрока
    private JLabel infoLabel;                        // Метка для отображения информации о текущем игроке
    private JButton rollButton;                      // Кнопка для броска кубиков
    private JButton exitButton;                      // Кнопка выхода
    private JPanel dicePanel;                        // Панель для отображения кубиков
    private Image[] diceImages = new Image[6];     // Массив изображений кубиков
    private int[] currentDice;                      // Текущий набор кубиков
    private int[] scoredDice;                       // Кубики, которые принесли очки
    private Timer timer;                             // Таймер для анимации
    private int animationFrame;                      // Для отслеживания кадров анимации
    private int remainingDiceCount;                 // Счетчик оставшихся кубиков

    public DiceGame() {
        loadResources(); // Загрузка изображений кубиков и фона
        setTitle("Dice Game");
        setSize(660, 300); // Увеличена ширина окна для отображения 6 кубиков
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        // Запрашиваем количество игроков
        int numPlayers = getNumberOfPlayers();
        players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            String name = JOptionPane.showInputDialog("Введите имя игрока " + (i + 1) + ":");
            players.add(new Player(name));
        }

        infoLabel = new JLabel();
        rollButton = new JButton("Бросить кубики");
        exitButton = new JButton("Выход"); // Добавлена кнопка выхода
        dicePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Отрисовка фона
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null); // Отрисовка фона на весь экран
                }
                if (currentDice != null) {
                    for (int i = 0; i < currentDice.length; i++) {
                        if (currentDice[i] >= 0) { // Проверяем, что кубик активен
                            if (diceImages[currentDice[i]] != null) {
                                g.drawImage(diceImages[currentDice[i]], i * 110 + 70, 50, 100, 150, null);
                            }
                        }
                    }
                } else {
                    g.drawString("Кубики не загружены", 50, 50);
                }
            }
        };

        dicePanel.setPreferredSize(new Dimension(660, 270)); // Увеличена ширина панели для размещения 6 кубиков

        add(infoLabel);
        add(rollButton);
        add(exitButton); // Добавлена кнопка выхода в интерфейс
        add(dicePanel);

        // Обработчик для броска кубиков
        rollButton.addActionListener(e -> {
            playSound("roul_sound.wav"); // Воспроизводим звук при броске
            startRollAnimation();
        });

        // Обработчик для выхода из игры
        exitButton.addActionListener(e -> System.exit(0)); // Завершить работу приложения

        currentPlayerIndex = 0;
        updateInfo();
    }

    private void loadResources() {
        // Загрузка изображений для кубиков
        for (int i = 0; i < 6; i++) {
            String path = "dice" + (i + 1) + ".png"; // Путь к изображениям кубиков
            diceImages[i] = loadImage(path);
        }
        // Загрузка изображения фона
        backgroundImage = loadImage("background.png"); // Укажите путь к изображению фона
    }

    private Image loadImage(String imagePath) {
        URL resourceUrl = getClass().getResource("/" + imagePath);
        if (resourceUrl != null) {
            return new ImageIcon(resourceUrl).getImage();
        } else {
            System.err.println("Изображение не найдено: " + imagePath);
            return null;
        }
    }

    private void playSound(String soundFile) {
        try {
            URL soundUrl = getClass().getResource("/" + soundFile); // Убедитесь, что путь правильный
            if (soundUrl == null) {
                System.err.println("Звук не найден: " + soundFile);
                return;
            }
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundUrl);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch (Exception e) {
            System.err.println("Ошибка воспроизведения звука: " + e.getMessage());
        }
    }

    private void startRollAnimation() {
        animationFrame = 0;
        currentDice = new int[DICE_COUNT]; // Сбрасываем текущие кубики
        remainingDiceCount = DICE_COUNT;    // Сбрасываем количество оставшихся кубиков
        scoredDice = new int[DICE_COUNT];   // Массив для хранения кубиков, которые принесли очки

        timer = new Timer(100, e -> {
            Random rand = new Random();
            for (int i = 0; i < remainingDiceCount; i++) {
                currentDice[i] = rand.nextInt(6); // Случайные значения для анимации
            }
            dicePanel.repaint(); // Перерисовываем панель

            animationFrame++;
            if (animationFrame >= ANIMATION_FRAMES) {
                timer.stop(); // Остановить анимацию
                currentDice = rollDice(); // Бросок кубиков
                dicePanel.repaint(); // Перерисовываем с настоящими значениями
                analyzeRoll(); // Анализируем бросок
            }
        });
        timer.start(); // Запускаем анимацию
    }

    private int getNumberOfPlayers() {
        while (true) {
            String input = JOptionPane.showInputDialog("Введите количество игроков (от 2 до 8):");
            try {
                int numPlayers = Integer.parseInt(input);
                if (numPlayers >= 2 && numPlayers <= 8) {
                    return numPlayers;
                } else {
                    JOptionPane.showMessageDialog(this, "Количество игроков должно быть от 2 до 8!");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Пожалуйста, введите корректное число.");
            }
        }
    }

    private void updateInfo() {
        Player currentPlayer = players.get(currentPlayerIndex);
        infoLabel.setText("Ход: " + currentPlayer.name + " (Очки: " + currentPlayer.totalScore + ")");
    }

    private void analyzeRoll() {
        Player currentPlayer = players.get(currentPlayerIndex);
        int score = calculateScore(currentDice);

        // Для отслеживания, какая сумма будет набрана в текущем броске
        int gainedScore = calculateScoreForScoredDice();

        // Если игрок получает 5 очков или больше, откладываем кубики
        if (gainedScore >= 5) {
            for (int i = 0; i < currentDice.length; i++) {
                if (currentDice[i] >= 0) { // Если кубик принес очки
                    scoredDice[i] = currentDice[i]; // Запоминаем кубик
                    currentDice[i] = -1; // Откладываем кубик (убираем из дальнейшего броска)
                }
            }
            currentPlayer.currentScore += gainedScore; // Заработок очков суммируется
            updateInfo();

            // Уменьшаем оставшиеся кубики на количество отложенных
            remainingDiceCount = calculateRemainingDiceCount();
            int decision = JOptionPane.showConfirmDialog(this,
                    currentPlayer.name + ", вы получили " + currentPlayer.currentScore + " очков. Бросить еще раз?",
                    "Продолжение хода", JOptionPane.YES_NO_OPTION);

            if (decision == JOptionPane.YES_OPTION) {
                // Игрок продолжает бросать оставшиеся кубики
                startRollAnimation();
            } else {
                // Игрок завершает ход
                currentPlayer.totalScore += currentPlayer.currentScore;
                checkWinCondition(currentPlayer);
                currentPlayer.currentScore = 0; // Сбрасываем текущие очки
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size(); // Переход к следующему игроку
                updateInfo();
            }
        } else {
            // Если заработанные очки меньше 5, сбросить текущие очки
            JOptionPane.showMessageDialog(this, "Нулевая комбинация! Ваши очки сгорают.");
            currentPlayer.currentScore = 0; // Сбрасываем текущие очки
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size(); // Переход к следующему игроку
            updateInfo();
        }
    }

    private int calculateRemainingDiceCount() {
        // Подсчет оставшихся кубиков
        int count = 0;
        for (int die : currentDice) {
            if (die >= 0) count++; // Считаем количество оставшихся кубиков
        }
        return count; // Возвращаем количество оставшихся кубиков
    }

    private int calculateScoreForScoredDice() {
        // Подсчет очков для отложенных кубиков
        int score = 0;
        for (int die : currentDice) {
            if (die >= 0) { // Если кубик не отложен
                score += calculateScore(new int[]{die}); // Учитываем очки только для активных кубиков
            }
        }
        return score; // Возвращаем сумму очков
    }

    private void checkWinCondition(Player currentPlayer) {
        if (currentPlayer.totalScore >= WINNING_SCORE) {
            JOptionPane.showMessageDialog(this, currentPlayer.name + " выиграл с " + currentPlayer.totalScore + " очками!");
            System.exit(0); // Завершение игры
        }
    }

    private int[] rollDice() {
        Random rand = new Random();
        int[] dice = new int[remainingDiceCount]; // Используем оставшиеся кубики для нового броска
        for (int i = 0; i < remainingDiceCount; i++) {
            dice[i] = rand.nextInt(6); // Индексы изображений кубиков
        }
        return dice; // Возвращаем массив с новыми значениями кубиков
    }

    private int calculateScore(int[] dice) {
        HashMap<Integer, Integer> countMap = new HashMap<>();
        for (int die : dice) {
            countMap.put(die, countMap.getOrDefault(die, 0) + 1);
        }

        int score = 0;
        score += countMap.getOrDefault(1, 0) * 10; // Один 1
        score += countMap.getOrDefault(5, 0) * 5;  // Один 5

        // Учет комбинаций по 3 и более
        for (int i = 1; i <= 6; i++) {
            int count = countMap.getOrDefault(i, 0);
            if (count >= 3) {
                score += (i * 10) * (count / 3); // Комбинации по 3 и более
            }
        }

        return score; // Возвращаем итоговую сумму очков
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DiceGame app = new DiceGame();
            app.setVisible(true);
        });
    }
}
