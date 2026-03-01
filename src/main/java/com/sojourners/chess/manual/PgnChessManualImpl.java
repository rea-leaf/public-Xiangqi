package com.sojourners.chess.manual;

import com.sojourners.chess.board.ChessBoard;
import com.sojourners.chess.model.ManualRecord;
import com.sojourners.chess.util.StringUtils;
import com.sojourners.chess.util.XiangqiUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PgnChessManualImpl implements ChessManualService {

    @Override
    public ChessManual openChessManual(File file) {
        ChessManual cm = new ChessManual();
        ManualRecord head = new ManualRecord(0, "开始局面", 0);
        cm.setHead(head);
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String[] lines = content.split("\\R");
            String format = "Chinese";
            int i = 0;
            for (; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (!line.startsWith("[")) {
                    break;
                }
                int sp = line.indexOf(' ');
                int q1 = line.indexOf('"');
                int q2 = line.lastIndexOf('"');
                if (sp > 1 && q1 > sp && q2 > q1) {
                    String tag = line.substring(1, sp).trim();
                    String val = line.substring(q1 + 1, q2);
                    switch (tag) {
                        case "Event" -> cm.setName(val);
                        case "Site" -> cm.setCity(val);
                        case "Date" -> cm.setDate(val);
                        case "Red" -> cm.setRed(val);
                        case "Black" -> cm.setBlack(val);
                        case "FEN" -> cm.setFenCode(val);
                        case "Format" -> format = val;
                        default -> {}
                    }
                }
            }
            StringBuilder moveText = new StringBuilder();
            for (int j = i; j < lines.length; j++) {
                moveText.append(lines[j]).append('\n');
            }
            String s = moveText.toString();
            int idx = 0;
            ManualRecord current = head;
            ManualRecord last = null;
            while (idx < s.length()) {
                char c = s.charAt(idx);
                if (Character.isWhitespace(c)) {
                    idx++;
                    continue;
                }
                if (c == '{') {
                    int end = s.indexOf('}', idx + 1);
                    if (end == -1) {
                        break;
                    }
                    String remark = s.substring(idx + 1, end).trim();
                    if (last != null) {
                        last.setRemark(remark);
                    } else {
                        head.setRemark(remark);
                    }
                    idx = end + 1;
                    continue;
                }
                if (Character.isDigit(c)) {
                    // skip move number "n." possibly with spaces
                    int dot = s.indexOf('.', idx);
                    if (dot == -1) {
                        // not a proper move number, fall through
                    } else {
                        idx = dot + 1;
                        continue;
                    }
                }
                // read token until whitespace or brace
                int start = idx;
                while (idx < s.length()) {
                    char ch = s.charAt(idx);
                    if (Character.isWhitespace(ch) || ch == '{' || ch == '}') break;
                    idx++;
                }
                if (idx <= start) {
                    idx++;
                    continue;
                }
                String token = s.substring(start, idx);
                String t = token.trim();
                if (t.isEmpty()) {
                    continue;
                }
                // result tokens
                if ("1-0".equals(t) || "0-1".equals(t) || "1/2-1/2".equals(t) || "*".equals(t)) {
                    break;
                }
                // ICCS move: A0-A1 or a0a1
                String lower = t.toLowerCase();
                boolean iccsHyphen = lower.matches("^[a-i][0-9]-[a-i][0-9]$");
                boolean iccsPlain = lower.matches("^[a-i][0-9][a-i][0-9]$");
                if (iccsHyphen || iccsPlain || "ICCS".equals(format)) {
                    String norm = lower.replace("-", "");
                    ManualRecord mr = new ManualRecord(last == null ? 1 : last.getId() + 1, norm, "");
                    current.getList().add(mr);
                    current.setNext(0);
                    current = mr;
                    last = mr;
                } else if ("Chinese".equals(format)) {
                    ManualRecord mr = new ManualRecord(last == null ? 1 : last.getId() + 1, "", t);
                    current.getList().add(mr);
                    current.setNext(0);
                    current = mr;
                    last = mr;
                }
                // unknown token, skip
            }
            if (cm.getFenCode().contains(" r ")) {
                cm.setFenCode(cm.getFenCode().replace(" r ", " w "));
            }
            if ("Chinese".equals(format)) {
                translateCnMove(cm.getFenCode(), cm.getHead());
            } else if ("ICCS".equals(format)) {
                translate(cm.getFenCode(), cm.getHead());
            }
            return cm;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void translateCnMove(String fenCode, ManualRecord mr) {
        char[][] board = XiangqiUtils.fenToBoard(fenCode);
        while (mr != null) {
            String cnMove = mr.getCnMove();
            if (StringUtils.isNotEmpty(cnMove) && mr.getId() > 0) {
                StringBuilder sb = new StringBuilder();
                ChessBoard.Step step = XiangqiUtils.translateCnMove(board, sb, cnMove);
                mr.setMove(sb.toString());

                int fromI = step.getStart().getY(), toI = step.getEnd().getY();
                int fromJ = step.getStart().getX(), toJ = step.getEnd().getX();
                board[toI][toJ] = board[fromI][fromJ];
                board[fromI][fromJ] = ' ';
            }
            mr = mr.getList().isEmpty() ? null : mr.getList().get(mr.getNext());
        }
    }

    private void translate(String fenCode, ManualRecord mr) {
        char[][] board = XiangqiUtils.fenToBoard(fenCode);
        while (mr != null) {
            String move = mr.getMove();
            if (StringUtils.isNotEmpty(move)) {
                StringBuilder sb = new StringBuilder();
                ChessBoard.Step step = XiangqiUtils.translate(board, sb, move, false);
                mr.setCnMove(sb.toString());

                int fromI = step.getStart().getY(), toI = step.getEnd().getY();
                int fromJ = step.getStart().getX(), toJ = step.getEnd().getX();
                board[toI][toJ] = board[fromI][fromJ];
                board[fromI][fromJ] = ' ';
            }
            mr = mr.getList().isEmpty() ? null : mr.getList().get(mr.getNext());
        }
    }

    @Override
    public void saveChessManual(ChessManual cm, File file) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Game \"Chinese Chess\"]").append(System.lineSeparator());
        sb.append("[Event \"" + cm.getName() + "\"]").append(System.lineSeparator());
        sb.append("[Site \"" + cm.getCity() + "\"]").append(System.lineSeparator());
        sb.append("[Date \"" + cm.getDate() + "\"]").append(System.lineSeparator());
        sb.append("[Red \"" + cm.getRed() + "\"]").append(System.lineSeparator());
        sb.append("[Black \"" + cm.getBlack() + "\"]").append(System.lineSeparator());
        sb.append("[Result \"*\"]").append(System.lineSeparator());
        sb.append("[FEN \"" + cm.getFenCode() + "\"]").append(System.lineSeparator());
        sb.append("[Format \"ICCS\"]").append(System.lineSeparator());
        ManualRecord h = cm.getHead();
        if (StringUtils.isNotEmpty(h.getRemark())) {
            sb.append("{" + h.getRemark() + "}").append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
        while (!h.getList().isEmpty()) {
            h = h.getList().get(h.getNext());
            sb.append(generate(h));
        }
        sb.append("*");
        try {
            Files.writeString(file.toPath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generate(ManualRecord record) {
        StringBuilder sb = new StringBuilder();
        int p = record.getId();
        if (p % 2 == 1) {
            sb.append((p + 1) / 2).append(". ");
        } else {
            sb.append("    ");
        }
        String move = record.getMove().toUpperCase();
        sb.append(move.substring(0, 2) + "-" + move.substring(2) + " ");
        if (StringUtils.isNotEmpty(record.getRemark())) {
            sb.append("{" + record.getRemark() + "}");
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}