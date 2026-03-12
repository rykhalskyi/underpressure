import cv2
import numpy as np
import imutils

IMAGE_PATH = "5.jpg"

DIGITS_LOOKUP = {
(1,1,1,1,1,1,0):0,   # top, tr, br, bottom, bl, tl, no-middle
(0,1,1,0,0,0,0):1,   # no-top, tr, br, no-bottom, no-bl, no-tl, no-middle
(1,1,0,1,1,0,1):2,   # top, tr, no-br, bottom, bl, no-tl, middle
(1,1,1,1,0,0,1):3,   # top, tr, br, bottom, no-bl, no-tl, middle
(1,1,1,1,1,0,1):3,   # alternative 3: top, tr, br, bottom, bl, no-tl, middle
(0,1,1,0,0,1,1):4,   # no-top, tr, br, no-bottom, no-bl, tl, middle
(1,0,1,1,0,1,1):5,   # top, no-tr, br, bottom, no-bl, tl, middle
(1,0,1,1,1,1,1):6,   # top, no-tr, br, bottom, bl, tl, middle
(1,1,1,0,0,0,0):7,   # top, tr, br, no-bottom, no-bl, no-tl, no-middle
(1,1,1,1,1,1,1):8,   # all segments on
(1,1,1,1,0,1,1):9    # top, tr, br, bottom, no-bl, tl, middle
}

def four_point_transform(image, pts):

    pts = pts.reshape(4,2)
    rect = np.zeros((4,2), dtype="float32")

    s = pts.sum(axis=1)
    rect[0] = pts[np.argmin(s)]
    rect[2] = pts[np.argmax(s)]

    diff = np.diff(pts, axis=1)
    rect[1] = pts[np.argmin(diff)]
    rect[3] = pts[np.argmax(diff)]

    (tl,tr,br,bl) = rect

    widthA = np.linalg.norm(br-bl)
    widthB = np.linalg.norm(tr-tl)
    maxWidth = int(max(widthA,widthB))

    heightA = np.linalg.norm(tr-br)
    heightB = np.linalg.norm(tl-bl)
    maxHeight = int(max(heightA,heightB))

    dst = np.array([
        [0,0],
        [maxWidth-1,0],
        [maxWidth-1,maxHeight-1],
        [0,maxHeight-1]
    ], dtype="float32")

    M = cv2.getPerspectiveTransform(rect,dst)

    return cv2.warpPerspective(image,M,(maxWidth,maxHeight))

#####

img_original = cv2.imread(IMAGE_PATH)
if img_original is None:
    raise ValueError(f"Could not read image from {IMAGE_PATH}")

print("start. make gray")
gray = cv2.cvtColor(img_original, cv2.COLOR_BGR2GRAY)
blur = cv2.GaussianBlur(gray,(5,5),0)
edges = cv2.Canny(blur,50,200)

#cv2.imshow("edges",edges)

contours,_ = cv2.findContours(
    edges,
    cv2.RETR_EXTERNAL,
    cv2.CHAIN_APPROX_SIMPLE
)

contours = sorted(contours,key=cv2.contourArea,reverse=True)

displayCnt = None

for c in contours:
    peri = cv2.arcLength(c,True)
    approx = cv2.approxPolyDP(c,0.02*peri,True)

    if len(approx) == 4:
        displayCnt = approx
        break

warped = four_point_transform(gray, displayCnt)

thresh = cv2.threshold(warped, 33, 255, cv2.THRESH_BINARY_INV)[1]
kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (1, 5))
thresh = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, kernel)

image = imutils.resize(thresh, height=500)
image = cv2.GaussianBlur(image,(7,7),0)

#cv2.imshow("image",image)

cnts = cv2.findContours(image.copy(), cv2.RETR_EXTERNAL,
	cv2.CHAIN_APPROX_SIMPLE)

cnts = imutils.grab_contours(cnts)
digitCnts = []
# loop over the digit area candidates
for c in cnts:
    # compute the bounding box of the contour
    (x, y, w, h) = cv2.boundingRect(c)
    print(f"rec x:{x} y:{y} w:{w} h:{h}")
    # if the contour is sufficiently large, it must be a digit
    if (w >= 20 and w <=125) and (h >= 70 and h <= 138):
        digitCnts.append(c)

# Group digits by height with tolerance of ±3 pixels
height_groups = {}
contours_with_heights = []

for c in digitCnts:
    (x, y, w, h) = cv2.boundingRect(c)
    contours_with_heights.append((c, h))

# Assign each contour to a height group (with ±3 pixel tolerance)
for c, h in contours_with_heights:
    assigned = False
    for group_height in height_groups:
        if abs(h - group_height) <= 3:
            height_groups[group_height].append(c)
            assigned = True
            break
    if not assigned:
        height_groups[h] = [c]

# Remove groups with only 1 digit (likely glitches)
digitCnts = []
for group_height, contours in height_groups.items():
    if len(contours) > 1:
        digitCnts.extend(contours)
        print(f"Height {group_height}: {len(contours)} digits (kept)")
    else:
        print(f"Height {group_height}: 1 digit (removed as glitch)")

print(f"Found {len(digitCnts)} digit contours")
print(f"Total contours found: {len(cnts)}")

# Draw bounding boxes on a copy of the image
image_with_boxes = cv2.cvtColor(image, cv2.COLOR_GRAY2BGR)
for c in digitCnts:
	(x, y, w, h) = cv2.boundingRect(c)
	print(f"Drawing box at ({x}, {y}) with width {w}, height {h}")
	cv2.rectangle(image_with_boxes, (x, y), (x + w, y + h), (0, 255, 0), 2)

# Also draw all contours to see all of them
for c in cnts:
	(x, y, w, h) = cv2.boundingRect(c)
	cv2.rectangle(image_with_boxes, (x, y), (x + w, y + h), (255, 0, 0), 1)

cv2.imshow("digits with boxes", image_with_boxes)


digitsSorted = sorted(digitCnts, key=lambda c: cv2.boundingRect(c)[0])

# Recognize and display digits
digit_debug = cv2.cvtColor(image, cv2.COLOR_GRAY2BGR)
recognized_digits_with_pos = []  # Store (x, y, digit)

for c in digitsSorted:
    x, y, w, h = cv2.boundingRect(c)
    cv2.rectangle(digit_debug, (x, y), (x + w, y + h), (0, 255, 0), 2)
    
    # Extract ROI for this digit
    roi = image[y:y + h, x:x + w]
    
    # Define 7 segments (top, top-right, bottom-right, bottom, bottom-left, top-left, middle)
    segments = [
        ((0.2*w, 0), (0.8*w, 0.2*h)),           # top
        ((0.8*w, 0.2*h), (w, 0.5*h)),           # top-right
        ((0.8*w, 0.5*h), (w, 0.9*h)),           # bottom-right
        ((0.2*w, 0.8*h), (0.8*w, h)),           # bottom
        ((0, 0.5*h), (0.2*w, 0.9*h)),           # bottom-left
        ((0, 0.2*h), (0.2*w, 0.5*h)),           # top-left
        ((0.2*w, 0.45*h), (0.8*w, 0.55*h))      # middle
    ]
    
    on = []
    
    # Check each segment
    for (p1, p2) in segments:
        x1, y1 = int(p1[0]), int(p1[1])
        x2, y2 = int(p2[0]), int(p2[1])
        
        seg = roi[y1:y2, x1:x2]
        
        area = (x2 - x1) * (y2 - y1)
        total = cv2.countNonZero(seg)
        
        ratio = total / area if area > 0 else 0
        
        if ratio > 0.25:
            on.append(1)
        else:
            on.append(0)
        
        # Draw segment rectangles for debugging
        cv2.rectangle(
            digit_debug,
            (x + x1, y + y1),
            (x + x2, y + y2),
            (255, 0, 0),
            1
        )
    
    # Look up the digit from the pattern
    digit = DIGITS_LOOKUP.get(tuple(on), "?")
    
    # Special rule for digit 1: width should be significantly less than height
    if w < 0.25 * h:
        digit = 1
    
    recognized_digits_with_pos.append((x, y, digit))
    
    print(f"Box: w={w}, h={h}, ratio={w/h:.2f}. Segments: {on}, Digit: {digit}")
    
    # Display recognized digit on image
    cv2.putText(
        digit_debug,
        str(digit),
        (x, y - 5),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.7,
        (0, 0, 255),
        2
    )

cv2.imshow("Recognized Digits", digit_debug)

# Group digits by row (y position with ±3 pixel tolerance)
row_groups = {}
for x, y, digit in recognized_digits_with_pos:
    assigned = False
    for group_y in row_groups:
        if abs(y - group_y) <= 3:
            row_groups[group_y].append((x, digit))
            assigned = True
            break
    if not assigned:
        row_groups[y] = [(x, digit)]

# Sort each row by x position and print
print("\n" + "="*50)
print("Recognized number by rows:")
print("="*50)
for y in sorted(row_groups.keys()):
    row_digits = sorted(row_groups[y], key=lambda item: item[0])
    row_number = ''.join(str(digit) for _, digit in row_digits)
    print(f"Row (y={y}): {row_number}")

#####


print("start")
#cv2.namedWindow("4_threshold")
# cv2.createTrackbar("threshold","4_threshold",120,255,update)

#process(120)

while True:
    if cv2.waitKey(1) == 27:
        break

cv2.destroyAllWindows()