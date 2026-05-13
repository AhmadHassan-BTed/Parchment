import os
import fitz  # This is PyMuPDF

def batch_compress_raster(quality_dpi=72):
    """
    Re-creates the PDF by converting pages to compressed images.
    quality_dpi: 
        72  = Low quality (Smallest Size, good for mobile 'Screen')
        150 = Medium quality (Better text, larger size)
    """
    current_dir = os.path.dirname(os.path.abspath(__file__))
    output_dir = os.path.join(current_dir, "compressed_output")

    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    print(f"Starting FULL Compression (Target DPI: {quality_dpi})...")
    print(f"{'File Name':<20} | {'Original':<10} | {'New Size':<10} | {'Saved'}")
    print("-" * 65)

    for filename in os.listdir(current_dir):
        if filename.lower().endswith(".pdf"):
            input_path = os.path.join(current_dir, filename)
            output_path = os.path.join(output_dir, filename)
            
            try:
                # Open the original PDF
                doc = fitz.open(input_path)
                new_doc = fitz.open() # Create a new empty PDF

                orig_size = os.path.getsize(input_path) / 1024

                for page in doc:
                    # 1. Render the page to an image (Pixmap) at specific DPI
                    # matrix controls the resolution (72 dpi is standard scale=1)
                    zoom = quality_dpi / 72
                    mat = fitz.Matrix(zoom, zoom)
                    pix = page.get_pixmap(matrix=mat, alpha=False)

                    # 2. Compress the image data (JPEG quality 65 is the sweet spot)
                    # This is where the magic happens - shrinking the bytes
                    img_data = pix.tobytes("jpeg", jpg_quality=65)

                    # 3. Create a new page in the new PDF with the same dimensions
                    new_page = new_doc.new_page(width=page.rect.width, height=page.rect.height)
                    
                    # 4. Insert the compressed image onto the new page
                    new_page.insert_image(new_page.rect, stream=img_data)

                # Save the new "Rasterized" PDF
                new_doc.save(output_path, garbage=4, deflate=True)
                
                new_size = os.path.getsize(output_path) / 1024
                saved = orig_size - new_size
                percent = (saved / orig_size) * 100 if orig_size > 0 else 0

                print(f"{filename:<20} | {orig_size:>7.1f} KB | {new_size:>7.1f} KB | {percent:>5.1f}%")

            except Exception as e:
                print(f"Error processing {filename}: {e}")

    print("\nProcessing Complete!")

if __name__ == "__main__":
    # TRY 72 FIRST. If it's too blurry, change to 110 or 150.
    batch_compress_raster(quality_dpi=150)