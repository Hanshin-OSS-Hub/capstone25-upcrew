from ultralytics import YOLO
import cv2
import matplotlib.pyplot as plt

model = YOLO("yolo11x.pt")


def is_overlapping(box_a, box_b, threshold=0.05):
    """box_b가 box_a(테이블) 영역과 겹치는지 확인."""
    ax1, ay1, ax2, ay2 = box_a
    bx1, by1, bx2, by2 = box_b

    ix1 = max(ax1, bx1)
    iy1 = max(ay1, by1)
    ix2 = min(ax2, bx2)
    iy2 = min(ay2, by2)

    if ix2 <= ix1 or iy2 <= iy1:
        return False

    inter_area = (ix2 - ix1) * (iy2 - iy1)
    b_area = (bx2 - bx1) * (by2 - by1)
    if b_area == 0:
        return False

    return (inter_area / b_area) > threshold


def expand_box(box, ratio=0.3):
    x1, y1, x2, y2 = box
    w = x2 - x1
    h = y2 - y1
    pad_x = int(w * ratio)
    pad_y = int(h * ratio)
    return (x1 - pad_x, y1 - pad_y, x2 + pad_x, y2 + pad_y)


def analyze(image_path: str) -> dict:
    """
    analyzer_worker.py에서 호출하는 함수.
    반환: {"level": str, "score": float, "count": int}
    """
    res = model(image_path, imgsz=1280, conf=0.20)

    by_cls = {0: [], 24: [], 60: []}
    for box in res[0].boxes:
        c = int(box.cls[0])
        if c in by_cls:
            by_cls[c].append(tuple(map(int, box.xyxy[0])))

    t_status = []
    for (tx1, ty1, tx2, ty2) in by_cls[60]:
        u = any(
            is_overlapping(
                (tx1, ty1, tx2, ty2),
                expand_box(obox) if cls == 24 else obox
            )
            for cls in [0, 24] for obox in by_cls[cls]
        )
        t_status.append(u)

    total = len(t_status)
    used  = sum(t_status)
    s     = used / total if total else 0.0
    lvl   = "혼잡" if s >= 0.7 else ("보통" if s >= 0.4 else "여유")

    return {"level": lvl, "score": round(s, 4), "count": len(by_cls[0])}


# 이 파일을 직접 실행할 때만 동작하는 테스트 코드 (python congestion_analysis.py)
if __name__ == "__main__":
    img = "test4.jpg"

    results = model(img, imgsz=1280, conf=0.20)

    annotated = results[0].orig_img.copy()

    # person(0), backpack(24), dining table(60)
    TARGET_CLASSES = [0, 24, 60]

    boxes_by_class = {cls: [] for cls in TARGET_CLASSES}

    for box in results[0].boxes:
        cls = int(box.cls[0])
        conf = float(box.conf[0])
        if cls not in TARGET_CLASSES:
            continue
        x1, y1, x2, y2 = map(int, box.xyxy[0])
        boxes_by_class[cls].append((x1, y1, x2, y2, conf))

    # 테이블별 Used/Free 판별
    table_status = []
    for (tx1, ty1, tx2, ty2, tconf) in boxes_by_class[60]:
        used = False
        for occupant_cls in [0, 24]:
            for (ox1, oy1, ox2, oy2, oconf) in boxes_by_class[occupant_cls]:
                box = expand_box((ox1, oy1, ox2, oy2)) if occupant_cls == 24 else (ox1, oy1, ox2, oy2)
                if is_overlapping((tx1, ty1, tx2, ty2), box):
                    used = True
                    break
            if used:
                break
        table_status.append(((tx1, ty1, tx2, ty2), used))

    # 테이블 그리기 (Used=빨강, Free=노랑)
    for (tx1, ty1, tx2, ty2), used in table_status:
        color = (0, 0, 255) if used else (0, 255, 255)   # BGR: 빨강 / 노랑
        label = "Table: Used" if used else "Table: Free"
        cv2.rectangle(annotated, (tx1, ty1), (tx2, ty2), color, 2)
        cv2.putText(annotated, label, (tx1, ty1 - 10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

    # person / backpack 그리기 (초록)
    for occupant_cls in [0, 24]:
        for (ox1, oy1, ox2, oy2, oconf) in boxes_by_class[occupant_cls]:
            label = "person" if occupant_cls == 0 else "backpack"
            cv2.rectangle(annotated, (ox1, oy1), (ox2, oy2), (0, 255, 0), 2)
            cv2.putText(annotated, f"{label} {oconf:.2f}", (ox1, oy1 - 10),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

    # 혼잡도 계산
    total_tables = len(table_status)
    used_tables  = sum(1 for _, used in table_status if used)
    free_tables  = total_tables - used_tables
    people_count = len(boxes_by_class[0])

    if total_tables == 0:
        level = "정보 없음"
        score = 0.0
    else:
        score = used_tables / total_tables
        if score >= 0.7:
            level = "혼잡"
        elif score >= 0.4:
            level = "보통"
        else:
            level = "여유"

    print(f"전체 테이블: {total_tables}")
    print(f"사용 중:    {used_tables}")
    print(f"비어있음:   {free_tables}")
    print(f"감지 인원:  {people_count}명")
    print(f"혼잡도:     {level} ({score*100:.0f}%)")

    cv2.imwrite("result_congestion.png", annotated)
    print("저장 완료: result_congestion.png")

    plt.imshow(cv2.cvtColor(annotated, cv2.COLOR_BGR2RGB))
    plt.axis("off")
    plt.title(f"혼잡도: {level} | 사용 {used_tables}/{total_tables} 테이블")
    plt.show()
