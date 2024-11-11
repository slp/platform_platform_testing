#!/usr/bin/env python3

import argparse
import os
import shutil


def parse_arguments():
    """Parses command-line arguments and returns the parsed arguments object."""
    parser = argparse.ArgumentParser(description="Validate directories and golden files.")
    parser.add_argument("--source-directory", required=True, help="Path to the source directory.")
    parser.add_argument("--android-build-top", required=True,
                        help="Path to the Android build directory.")
    return parser.parse_args()


def validate_directories(args):
    """Validates the provided source and Android directory arguments and returns their paths if valid."""
    if not args.source_directory or not args.android_build_top:
        print("Error: Both --source-directory and --android-build-top arguments are required.")
        return None

    source_dir = args.source_directory
    android_dir = args.android_build_top

    is_source_dir_valid = os.path.isdir(source_dir)
    is_android_dir_valid = os.path.isdir(android_dir)

    if not is_source_dir_valid:
        print(f"Error: Source directory does not exist: {source_dir}")

    if not is_android_dir_valid:
        print(f"Error: Android build directory does not exist: {android_dir}")

    if not is_source_dir_valid or not is_android_dir_valid:
        return None

    return [source_dir, android_dir]


def find_golden_files(source_dir):
    """Finds golden files within the source directory and returns their filenames or handles errors."""
    golden_files = []
    for root, _, files in os.walk(source_dir):
        for file in files:
            if "_goldResult_" in file and file.endswith(".textproto"):
                golden_files.append(file)

    if not golden_files:
        print("Error: No golden files found in the source directory.")
        return None

    return golden_files


def validate_protos(source_dir, proto_files):
    """Validates proto files, extracts image locations, and handles errors."""
    all_image_locations = []
    for filename in proto_files:
        required_image_locations = {"image_location_diff": False, "image_location_golden": False,
                                    "image_location_reference": False, "image_location_test": False}
        found_image_locations = {}

        with open(os.path.join(source_dir, filename), 'r') as file:
            for line in file:
                for location in required_image_locations:
                    if line.startswith(location):
                        if required_image_locations[location]:
                            print(f"Error: Duplicate '{location}' entry found in {filename}.")
                            return None
                        required_image_locations[location] = True
                        found_image_locations[location] = line.split(": ")[1].strip().strip('"')
                        break

        if not all(required_image_locations.values()):
            missing_keys = [key for key, found in required_image_locations.items() if not found]
            print(f"Error: Missing image location(s) in {filename}: {', '.join(missing_keys)}")
            return None
        else:
            print(f"Proto file {filename} is valid.")
            all_image_locations.append(found_image_locations)
    return all_image_locations


def validate_test_images(source_dir, all_image_locations):
    """Validates if PNG files exist in the source directory based on provided image locations."""
    for image_locations in all_image_locations:
        for location, path in image_locations.items():
            if location != "image_location_golden":
                base_name = os.path.splitext(os.path.basename(path))[0]
                for root, _, files in os.walk(source_dir):
                    for file in files:
                        if file.startswith(base_name) and file.endswith(".png"):
                            image_locations[location] = os.path.join(root, file)
                            break
                    else:
                        print(f"Error: No PNG file found matching {path} in {source_dir}")
                        return None
        filename_without_ext = \
        os.path.splitext(os.path.basename(image_locations["image_location_golden"]))[0]
        print(f"Golden {filename_without_ext} is valid.")
    return all_image_locations


def update_goldens(android_dir, updated_image_locations_list):
    """Copies updated 'image_location_test' images to their 'image_location_golden' paths in the android_build_top directory."""
    for image_locations in updated_image_locations_list:
        test_image_path = image_locations["image_location_test"]
        golden_image_path = os.path.join(android_dir, image_locations["image_location_golden"])

        try:
            shutil.copy2(test_image_path, golden_image_path)
            print(f"Updated golden image: {golden_image_path}")
        except IOError as e:
            print(f"Error updating golden image: {e}")


def main():
    args = parse_arguments()

    directories = validate_directories(args)
    if directories is None:
        return

    source_dir, android_dir = directories

    proto_files = find_golden_files(source_dir)
    if proto_files is None:
        return

    all_image_locations = validate_protos(source_dir, proto_files)
    if all_image_locations is None:
        return

    updated_image_locations_list = validate_test_images(source_dir, all_image_locations)
    if updated_image_locations_list is None:
        return

    update_goldens(android_dir, updated_image_locations_list)


if __name__ == "__main__":
    main()
