<?php

namespace App\Support;

class PropertySelectionService
{
    /**
     * @param Properties<\App\Models\Product> $properties
     */
    public function acceptProductProperties(array $properties): void
    {
    }

    /**
     * @return Properties<\App\Models\Product>|Properties<\App\Models\Account>
     */
    public function getProductOrAccountProperties(): array
    {
        return [];
    }
}
