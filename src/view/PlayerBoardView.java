package view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.WindowConstants;

import model.PlayerBoard;
import model.TileCollection;

/**
 * Visual representation of an individual player's board
 *
 * @author jsnhlbr5
 */
public class PlayerBoardView extends JLayeredPane
{
    private PlayerBoard model;
    private JLabel scoreMarker;
    private JButton[] buildRowButtons;
    private JFrame frame;

    // Constants used to position tiles
    private static final int BUILD_ROW_X_POS = 298;
    private static final int BUILD_ROW_Y_POS = 264;
    private static final int BUILD_ROW_X_OFFSET = -66;
    private static final int BUILD_ROW_Y_OFFSET = 66;

    private static final int WALL_ROW_X_POS = 393;
    private static final int WALL_ROW_Y_POS = 265;
    private static final int WALL_ROW_X_OFFSET = 66;
    private static final int WALL_ROW_Y_OFFSET = 65;

    private static final int FLOOR_X_POS = 33;
    private static final int FLOOR_Y_POS = 645;
    private static final int FLOOR_X_OFFSET = 72;

    private static final int SCORE_X_POS = 34;
    private static final int SCORE_Y_POS = 0;
    private static final double SCORE_X_OFFSET = 34.35;
    private static final int[] SCORE_Y_OFFSETS = { 0, 40, 80, 120, 164, 208 };

    /**
     * Constructs a new visual representation of the given player board.
     *
     * @param m
     *            the logical model to use
     */
    public PlayerBoardView(PlayerBoard m)
    {
        model = m;

        this.setPreferredSize(new Dimension(750, 750));
        JLabel board = new JLabel(ViewUtils.getImageIcon("/img/PlayerBoard.png"));
        board.setBounds(0, 0, 750, 750);
        this.add(board, DEFAULT_LAYER);
        scoreMarker = new JLabel(ViewUtils.getImageIcon("/img/score.png"));
        scoreMarker.setBounds(SCORE_X_POS, SCORE_Y_POS, 30, 30); // Score is known to be 0
        this.add(scoreMarker, ViewUtils.SCORE_LAYER);

        // 5 build rows plus floor line
        buildRowButtons = new JButton[6];
        for (int i = 0; i < 5; ++i)
        {
            buildRowButtons[i] = new InvisibleButton(new PlaceTilesAction(i));
            buildRowButtons[i].setBounds(BUILD_ROW_X_POS + (BUILD_ROW_X_OFFSET * i),
                    BUILD_ROW_Y_POS + (BUILD_ROW_Y_OFFSET * i), 60 + (BUILD_ROW_X_OFFSET * -i), 60);
            this.add(buildRowButtons[i], ViewUtils.INTERFACE_LAYER);
        }
        buildRowButtons[5] = new InvisibleButton(new PlaceTilesAction(5));
        buildRowButtons[5].setBounds(FLOOR_X_POS, FLOOR_Y_POS, 60 + (FLOOR_X_OFFSET * 6), 60);
        this.add(buildRowButtons[5], ViewUtils.INTERFACE_LAYER);
        updateButtons();

        frame = new JFrame(model.player);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.add(this);
        frame.pack();
        frame.setResizable(false);
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(ViewUtils.class.getResource("/img/TEAL.png")));
    }

    /**
     * Overrides setVisible() to trigger on the parent frame instead.
     *
     * Also contains a "hack" to eliminate a visual bug.
     */
    @Override
    public void setVisible(boolean b)
    {
        // IDK why, but without this, sometimes there's a 10-pixel gap on the right and bottom
        this.setSize(750, 750);
        frame.pack();
        frame.setVisible(b);
    }

    /**
     * Updates build row, floor line, and wall tiles to match logical model.
     */
    public void updateTiles()
    {
        // Remove old tiles
        for (Component c : getComponentsInLayer(ViewUtils.TILE_LAYER))
            this.remove(c);

        TileCollection tc;
        ImageIcon tileImage;
        JLabel tile;
        for (int r = 0; r < 5; ++r)
        {
            tc = model.getBuildRowTiles(r);
            int count = tc.size();
            if (count > 0)
            {
                tileImage = ViewUtils.getImageIcon("/img/" + tc.getColor().name() + ".png");
                for (int c = 0; c < count; ++c)
                {
                    tile = new JLabel(tileImage);
                    tile.setBounds(BUILD_ROW_X_POS + (BUILD_ROW_X_OFFSET * c),
                            BUILD_ROW_Y_POS + (BUILD_ROW_Y_OFFSET * r), 60, 60);
                    this.add(tile, ViewUtils.TILE_LAYER);
                }
            }
        }

        tc = model.getFloorLineTiles();
        int count = Math.min(tc.size(), 7);
        for (int i = 0; i < count; ++i)
        {
            tileImage = ViewUtils.getImageIcon("/img/" + tc.get(i).name() + ".png");
            tile = new JLabel(tileImage);
            tile.setBounds(FLOOR_X_POS + (FLOOR_X_OFFSET * i), FLOOR_Y_POS, 60, 60);
            this.add(tile, ViewUtils.TILE_LAYER);
        }

        boolean[][] wall = model.getWall();
        for (int r = 0; r < 5; ++r)
        {
            for (int c = 0; c < 5; ++c)
            {
                if (wall[r][c])
                {
                    tileImage = ViewUtils.getImageIcon("/img/" + getColorForWallPos(r, c) + ".png");
                    tile = new JLabel(tileImage);
                    tile.setBounds(WALL_ROW_X_POS + (WALL_ROW_X_OFFSET * c), WALL_ROW_Y_POS + (WALL_ROW_Y_OFFSET * r),
                            60, 60);
                    this.add(tile, ViewUtils.TILE_LAYER);
                }
            }
        }

        this.repaint();
    }

    /**
     * Updates the score indicator to match the player's current score.
     */
    public void updateScore()
    {
        int score = model.getScore();
        scoreMarker.setBounds(SCORE_X_POS + (int) (Math.rint(SCORE_X_OFFSET * Math.max(score % 20 - 1, 0))),
                SCORE_Y_POS + SCORE_Y_OFFSETS[(score + 19) / 20], 30, 30);

        this.repaint();
    }

    /**
     * Updates build row and floor line buttons based on state.
     */
    public void updateButtons()
    {
        if (!model.hasSelectedTiles())
        {
            for (int i = 0; i < 6; ++i)
            {
                buildRowButtons[i].setEnabled(false);
            }
        }
        else
        {
            for (int i = 0; i < 5; ++i)
            {
                buildRowButtons[i].setEnabled(model.canAddTilesToRow(i));
            }
            buildRowButtons[5].setEnabled(true);
        }
    }

    /**
     * Updates the window title to reflect whether it is this player's turn.
     *
     * @param currentPlayer
     *            true to add text to indicate that it is this player's turn, false to return the window title to just
     *            the player name
     */
    public void updateTitle(boolean currentPlayer)
    {
        if (currentPlayer)
            frame.setTitle(model.player + " (Active Player)");
        else
            frame.setTitle(model.player);
    }

    /**
     * Used to determine tile color for wall positions
     *
     * @param row
     *            row number
     * @param col
     *            column number
     * @return the color name
     */
    private String getColorForWallPos(int row, int col)
    {
        // plus 5 because we want it to always be positive
        switch ((col - row + 5) % 5)
        {
            case 0:
                return "BLUE";
            case 1:
                return "YELLOW";
            case 2:
                return "RED";
            case 3:
                return "BLACK";
            case 4:
                return "TEAL";
            default:
                return "ERROR"; // This can never happen, switch input is %5
        }
    }

    private class PlaceTilesAction extends AbstractAction
    {
        public final int row;

        public PlaceTilesAction(int r)
        {
            row = r;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            model.addTilesToRow(row);
        }

    }
}
