# SVG Sanitization

---

## Table of Contents
1. [Overview](#1-overview)
2. [Security Risks](#2-security-risks)
3. [Sanitization Strategy](#3-sanitization-strategy)
4. [Allowed Elements](#4-allowed-elements)
5. [Allowed Attributes](#5-allowed-attributes)
6. [Blocked Content](#6-blocked-content)
7. [Configuration](#7-configuration)

---

## 1. Overview

SVG files can contain executable code, making them a potential vector for XSS (Cross-Site Scripting) and XXE (XML External Entity) attacks. The application sanitizes all uploaded SVG files using a whitelist approach before storing them.

**Service**: `SvgStorageService`

**Library**: Jsoup 1.18.1 (XML parser)

**Strategy**: Whitelist-based sanitization (only explicitly allowed elements and attributes are kept)

**Use Case**: Skill icons in the portfolio (uploaded via admin panel)

---

## 2. Security Risks

### 2.1 XSS via SVG

SVG files can contain JavaScript that executes when the SVG is rendered in a browser.

**Attack Vectors**:
- `<script>` elements embedded in SVG
- Event handlers (`onclick`, `onload`, `onerror`)
- `javascript:` URLs in `xlink:href` attributes
- CSS `expression()` in style attributes

**Example malicious SVG**:
```xml
<svg xmlns="http://www.w3.org/2000/svg">
  <script>alert('XSS')</script>
  <circle cx="50" cy="50" r="40" onclick="alert('XSS')"/>
  <a xlink:href="javascript:alert('XSS')">
    <text>Click me</text>
  </a>
</svg>
```

### 2.2 XXE via SVG

SVG is XML-based and can be exploited for XXE attacks.

**Attack Vectors**:
- DOCTYPE declarations with external entities
- Parameter entity references
- External DTD references

**Example XXE payload**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE svg [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<svg xmlns="http://www.w3.org/2000/svg">
  <text>&xxe;</text>
</svg>
```

---

## 3. Sanitization Strategy

### 3.1 Process Flow

1. **Pre-validation**: Check file size, extension, MIME type
2. **Pattern detection**: Reject files with dangerous patterns (before parsing)
3. **DOCTYPE removal**: Strip XML declaration and DOCTYPE (XXE prevention)
4. **Parse**: Parse SVG with Jsoup XML parser
5. **Element filtering**: Remove elements not in whitelist
6. **Attribute filtering**: Remove attributes not in whitelist
7. **Value sanitization**: Check attribute values for dangerous content
8. **Output**: Generate clean SVG with safe XML declaration

### 3.2 Whitelist Approach

Only explicitly allowed elements and attributes are preserved. Everything else is removed.

**Why whitelist over blacklist**:
- Blacklists can be bypassed with encoding tricks
- New attack vectors are discovered regularly
- Whitelist is more restrictive and secure by default
- SVG for icons has limited legitimate use of elements

---

## 4. Allowed Elements

Elements preserved during sanitization:

### 4.1 Structure Elements
| Element | Description |
|---------|-------------|
| `svg` | Root SVG container |
| `g` | Group container |
| `defs` | Definitions container |
| `symbol` | Reusable symbol definition |
| `use` | Reference to symbol/element |
| `title` | Accessible title |
| `desc` | Accessible description |

### 4.2 Shape Elements
| Element | Description |
|---------|-------------|
| `path` | Arbitrary path |
| `circle` | Circle shape |
| `ellipse` | Ellipse shape |
| `line` | Line segment |
| `polyline` | Connected line segments |
| `polygon` | Closed polygon |
| `rect` | Rectangle |

### 4.3 Gradient and Pattern Elements
| Element | Description |
|---------|-------------|
| `linearGradient` | Linear color gradient |
| `radialGradient` | Radial color gradient |
| `stop` | Gradient color stop |
| `pattern` | Repeating pattern |
| `clipPath` | Clipping path |
| `mask` | Masking element |

### 4.4 Filter Elements
| Element | Description |
|---------|-------------|
| `filter` | Filter container |
| `feGaussianBlur` | Blur effect |
| `feOffset` | Offset effect |
| `feBlend` | Blend effect |
| `feMerge` | Merge layers |
| `feMergeNode` | Merge node |
| `feColorMatrix` | Color transformation |
| `feFlood` | Flood fill |
| `feComposite` | Composite operation |

---

## 5. Allowed Attributes

### 5.1 General Attributes
```
id, class, style, viewBox, xmlns, xmlns:xlink
```

### 5.2 Dimension Attributes
```
width, height, x, y, x1, y1, x2, y2, cx, cy, r, rx, ry
```

### 5.3 Path and Shape Attributes
```
d, points
```

### 5.4 Presentation Attributes
```
fill, stroke, stroke-width, stroke-linecap, stroke-linejoin,
stroke-dasharray, stroke-dashoffset, stroke-opacity, fill-opacity,
opacity, transform, clip-path, mask, filter
```

### 5.5 Gradient Attributes
```
offset, stop-color, stop-opacity, gradientUnits, gradientTransform,
spreadMethod
```

### 5.6 Reference Attributes
```
xlink:href, href, preserveAspectRatio
```

### 5.7 Pattern Attributes
```
patternUnits, patternContentUnits, patternTransform
```

### 5.8 Filter Attributes
```
stdDeviation, dx, dy, result, in, in2, mode, flood-color, flood-opacity,
operator, k1, k2, k3, k4, type, values, color-interpolation-filters
```

---

## 6. Blocked Content

### 6.1 Blocked Elements (always removed)

| Element | Reason |
|---------|--------|
| `script` | JavaScript execution |
| `foreignObject` | Embeds external content |
| `iframe` | Embeds external pages |
| `embed` | Embeds external content |
| `object` | Embeds external objects |
| `a` | Links (can contain javascript:) |
| `image` | External image references |
| `animate` | Can trigger events |
| `animateTransform` | Can trigger events |
| `set` | Can trigger events |
| `text`, `tspan` | Potential XSS via text content |

### 6.2 Blocked Attributes (always removed)

| Pattern | Reason |
|---------|--------|
| `on*` (onclick, onload, etc.) | Event handlers |
| `href` with `javascript:` | Script execution |
| `href` with `data:` | Data URI injection |
| `xlink:href` with `javascript:` | Script execution |

### 6.3 Blocked Patterns (reject file)

The following patterns in raw SVG content cause the upload to be rejected:

```regex
(?i)(on\w+\s*=|javascript:|data:|vbscript:)
```

This catches:
- Event handlers: `onclick=`, `onload=`, `onerror=`
- JavaScript URLs: `javascript:alert()`
- Data URIs: `data:text/html,<script>`
- VBScript: `vbscript:msgbox()`

### 6.4 Blocked DOCTYPE and XML Declaration

Removed during sanitization:
```xml
<!DOCTYPE svg [...]>
<?xml version="1.0" ... ?>
```

A safe XML declaration is added back after sanitization:
```xml
<?xml version="1.0" encoding="UTF-8"?>
```

---

## 7. Configuration

### 7.1 Application Properties

```yaml
svg:
  storage:
    upload-dir: ${SVG_UPLOAD_DIR:uploads/icons}
    base-path: ${SVG_BASE_PATH:/uploads/icons}
    max-file-size: ${SVG_MAX_FILE_SIZE:102400}  # 100KB
    allowed-extensions:
      - svg
    allowed-mime-types:
      - image/svg+xml
```

### 7.2 File Size Limit

Maximum SVG file size: **100 KB**

This limit is intentionally small because:
- Icon SVGs should be optimized
- Larger files increase attack surface
- Prevents DoS via large file uploads

### 7.3 Validation Order

1. File not null/empty
2. No path traversal in filename
3. File size within limit
4. Extension is `.svg`
5. MIME type is `image/svg+xml`
6. Contains `<svg` tag
7. No dangerous patterns detected
8. Sanitization successful

---

## Related Documentation

- [File Storage](../features/file-storage.md) - General file upload security
- [Security Best Practices](./README.md) - Input validation section
- [Image Processing](../features/image-processing.md) - Image upload handling
